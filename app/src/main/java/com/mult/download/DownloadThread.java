package com.mult.download;

import android.content.Context;
import android.os.Process;
import android.util.Log;

import com.mult.download.util.DownLoadLog;
import com.mult.download.util.FileUtils;

import org.apache.http.conn.ConnectTimeoutException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;

import static android.content.ContentValues.TAG;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

class DownloadThread extends Thread {

    /**
     * The queue of download requests to service.
     */
    private final BlockingQueue<DownloadRequest> mQueue;

    /**
     * Used to tell the dispatcher to die.
     */
    private volatile boolean mQuit = false;


    /**
     * To Delivery call back response on main thread
     */
    private DownloadRequestQueue.CallBackDelivery mDelivery;

    /**
     * The buffer size used to stream the data
     */
    private final int BUFFER_SIZE = 3*1024;

    /**
     * How many times redirects happened during a download request.
     */
    private int mRedirectionCount = 0;

    /**
     * The maximum number of redirects.
     */
    private final int MAX_REDIRECTS = 5; // can't be more than 7.

    private final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    private final int HTTP_TEMP_REDIRECT = 307;

    private String localPath;
    private String fileName;

    private long fileTotalSize;
    private long mCurrentBytes;
    private boolean shouldAllowRedirects = true;

    private Timer mTimer;

    /**
     * Constructor take the dependency (DownloadRequest queue) that all the Dispatcher needs
     */
    DownloadThread(BlockingQueue<DownloadRequest> queue,
                   DownloadRequestQueue.CallBackDelivery delivery) {
        mQueue = queue;
        mDelivery = delivery;

    }

    @Override
    public void run() {

        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        mTimer = new Timer();
        while (true) {

            DownloadRequest request = null;
            try {
                //取走BlockingQueue里排在首位的对象,若BlockingQueue为空,阻断进入等待状态直到Blocking有新的对象被加入为止
                request = mQueue.take();
                Log.i("chyy", "DownLoadDispatcher  mQueue.take() " + " - Thread name - " + Thread.currentThread().getName());
                //apk is already exists
                if (initFirst(request)) return;


                mRedirectionCount = 0;
                shouldAllowRedirects = true;
                DownLoadLog.v("Download initiated for " + request.getDownloadId());
                updateDownloadState(request, DownloadManager.STATUS_STARTED);
                executeDownload(request, request.getUrl());

            } catch (InterruptedException e) {
                // We may have been interrupted because it was time to quit.
                if (mQuit) {
                    if (request != null) {
                        request.finish();
                        // don't remove files that have been downloaded sucessfully.
                        if (request.getDownloadState() != DownloadManager.STATUS_SUCCESSFUL) {
                            updateDownloadFailed(request, DownloadManager.ERROR_DOWNLOAD_CANCELLED, "Download cancelled");
                        }
                    }
                    mTimer.cancel();
                    return;
                }
            }
        }
    }

    void quit() {
        mQuit = true;
        interrupt();
    }


    private synchronized void executeDownload(DownloadRequest request, String downloadUrl) {


        // updateDownloadFailed(request, DownloadManager.ERROR_MALFORMED_URI, "MalformedURLException: URI passed is malformed.");
        HttpURLConnection conn = null;
        URL url;
        InputStream inputStream = null;
        RandomAccessFile randomAccessFile;

        try {
            url = new URL(downloadUrl);
            long localFileLength = FileUtils.getLocalFileSize(getFullFileName());
            long remoteFileLength = fileTotalSize;

            // 远程文件不存在
            if (remoteFileLength == -1l) {
                // Log.log("下载文件不存在...");
                updateDownloadFailed(request, DownloadManager.ERROR_UNHANDLED_HTTP_CODE, "Remote file not found");
                return;
            }

            request.setFileFullName(getFullFileName());
            randomAccessFile = new RandomAccessFile(getFullFileName(), "rwd");


            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(request.getRetryPolicy().getCurrentTimeout());
            conn.setReadTimeout(request.getRetryPolicy().getCurrentTimeout());
            conn.setRequestMethod("GET");

            // 本地文件存在
            randomAccessFile.seek(localFileLength);
            conn.setRequestProperty("Range", "bytes="
                    + localFileLength + "-" + remoteFileLength);

            inputStream = conn.getInputStream();


            HashMap<String, String> customHeaders = request.getCustomHeaders();
            if (customHeaders != null) {
                for (String headerName : customHeaders.keySet()) {
                    conn.addRequestProperty(headerName, customHeaders.get(headerName));
                }
            }

            // Status Connecting is set here before
            // urlConnection is trying to connect to destination.
            updateDownloadState(request, DownloadManager.STATUS_CONNECTING);

            final int responseCode = conn.getResponseCode();

            DownLoadLog.v("Response code obtained for downloaded Id "
                    + request.getDownloadId()
                    + " : httpResponse Code "
                    + responseCode);

            switch (responseCode) {
                case HTTP_PARTIAL:
                case HTTP_OK:
                    shouldAllowRedirects = false;
                    if (fileTotalSize > 1) {
                        transferData(request, inputStream, randomAccessFile);
                    } else {
                        updateDownloadFailed(request, DownloadManager.ERROR_DOWNLOAD_SIZE_UNKNOWN, "Transfer-Encoding not found as well as can't know size of download, giving up");
                    }
                    break;
                case HTTP_MOVED_PERM:
                case HTTP_MOVED_TEMP:
                case HTTP_SEE_OTHER:
                case HTTP_TEMP_REDIRECT:
                    // Take redirect url and call executeDownload recursively until
                    // MAX_REDIRECT is reached.
                    while (mRedirectionCount < MAX_REDIRECTS && shouldAllowRedirects) {
                        mRedirectionCount++;
                        DownLoadLog.v(TAG, "Redirect for downloaded Id " + request.getDownloadId());
                        final String location = conn.getHeaderField("Location");
                        executeDownload(request, location);
                    }

                    if (mRedirectionCount > MAX_REDIRECTS && shouldAllowRedirects) {
                        updateDownloadFailed(request, DownloadManager.ERROR_TOO_MANY_REDIRECTS, "Too many redirects, giving up");
                        return;
                    }
                    break;
                case HTTP_REQUESTED_RANGE_NOT_SATISFIABLE:
                    updateDownloadFailed(request, HTTP_REQUESTED_RANGE_NOT_SATISFIABLE, conn.getResponseMessage());
                    break;
                case HTTP_UNAVAILABLE:
                    updateDownloadFailed(request, HTTP_UNAVAILABLE, conn.getResponseMessage());
                    break;
                case HTTP_INTERNAL_ERROR:
                    updateDownloadFailed(request, HTTP_INTERNAL_ERROR, conn.getResponseMessage());
                    break;
                default:
                    updateDownloadFailed(request, DownloadManager.ERROR_UNHANDLED_HTTP_CODE, "Unhandled HTTP response:" + responseCode + " message:" + conn.getResponseMessage());
                    break;
            }
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
            // Retry.
            attemptRetryOnTimeOutException(request);
        } catch (ConnectTimeoutException e) {
            e.printStackTrace();
            attemptRetryOnTimeOutException(request);
        } catch (IOException e) {
            e.printStackTrace();
            updateDownloadFailed(request, DownloadManager.ERROR_HTTP_DATA_ERROR, "IOException " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }


    private synchronized void transferData(DownloadRequest request, InputStream in, RandomAccessFile randomAccessFile) {

        long localFileLength = FileUtils.getLocalFileSize(getFullFileName());
        mCurrentBytes = localFileLength;


        final byte data[] = new byte[BUFFER_SIZE];

        request.setDownloadState(DownloadManager.STATUS_RUNNING);
        DownLoadLog.v("Content Length: " + fileTotalSize + " for Download Id " + request.getDownloadId());
        Log.i("chyy", "transferData  Thread --" + Thread.currentThread().getName());

        for (; ; ) {
            if (request.isCancelled()) {
                DownLoadLog.v("Stopping the download as Download Request is cancelled for Downloaded Id " + request.getDownloadId());
                //  request.finish();
                // updateDownloadFailed(request, DownloadManager.ERROR_DOWNLOAD_CANCELLED, "Download cancelled");
                return;
            }
            int bytesRead = readFromResponse(request, data, in);


            if (fileTotalSize != -1 && fileTotalSize > 0) {
                int progress = (int) ((mCurrentBytes * 100) / fileTotalSize);
                updateDownloadProgress(request, progress, mCurrentBytes);
            }

            if (mCurrentBytes >= fileTotalSize) {
                updateDownloadComplete(request);
            }

            if (bytesRead == -1) { // success, end of stream already reached
                updateDownloadComplete(request);
                return;
            } else if (bytesRead == Integer.MIN_VALUE) {
                return;
            }

            if (writeDataToDestination(request, data, bytesRead, randomAccessFile)) {
                mCurrentBytes += bytesRead;
            } else {
                request.finish();
                updateDownloadFailed(request, DownloadManager.ERROR_FILE_ERROR, "Failed writing file");
                return;
            }
        }
    }

    private int readFromResponse(DownloadRequest request, byte[] data, InputStream entityStream) {
        try {
            return entityStream.read(data);
        } catch (IOException ex) {
            Log.i("chyy", "ReadFromResponse  " + ex.getMessage());
            if ("unexpected end of stream".equals(ex.getMessage())) {
                return -1;
            }
            updateDownloadFailed(request, DownloadManager.ERROR_HTTP_DATA_ERROR, "IOException: Failed reading response " + ex.getMessage());
            return Integer.MIN_VALUE;
        }
    }

    private boolean writeDataToDestination(DownloadRequest request, byte[] data, int bytesRead, RandomAccessFile randomAccessFile) {
        boolean successInWritingToDestination = true;
        try {
            randomAccessFile.write(data, 0, bytesRead);
        } catch (IOException ex) {
            updateDownloadFailed(request, DownloadManager.ERROR_FILE_ERROR, "IOException when writing download contents to the destination file");
            successInWritingToDestination = false;
        }

        return successInWritingToDestination;
    }


    private void attemptRetryOnTimeOutException(final DownloadRequest request) {
        updateDownloadState(request, DownloadManager.STATUS_RETRYING);
        final RetryPolicy retryPolicy = request.getRetryPolicy();
        try {
            retryPolicy.retry();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    executeDownload(request, request.getUrl().toString());
                }
            }, retryPolicy.getCurrentTimeout());
        } catch (RetryError e) {
            // Update download failed.
            updateDownloadFailed(request, DownloadManager.ERROR_CONNECTION_TIMEOUT_AFTER_RETRIES,
                    "Connection time out after maximum retires attempted");
        }
    }


    //第一次下载初始化
    private boolean initFirst(DownloadRequest request) {

        try {
            URL url = new URL(request.getUrl());

            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            String[] tempArray = request.getUrl().split("/");

            this.fileName = cutPackagename(request.getContext()) + tempArray[tempArray.length - 1];

            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");

            localPath = request.getLocalPath();
            fileTotalSize = connection.getContentLength();
            // Log.w(TAG, "fileSize::" + fileSize);

            // Log.i("chyy", "fileTotalsize - " + fileTotalSize + " length : " + FileUtils.getLocalFileSize(getFullFileName()) + " isValiable " + FileUtils.getFileSize(new File(getFullFileName())));
            File fileParent = new File(localPath);
            if (!fileParent.exists()) {
                if (!fileParent.mkdirs()) {
                    localPath = request.getContext().getCacheDir().getPath();
                    fileParent = new File(localPath);
                    if (!fileParent.exists()) fileParent.mkdirs();

                }
            } else if (FileUtils.getLocalFileSize(getFullFileName()) >= fileTotalSize) {
                //下载路径，已经存在安装包
                updateDownloadComplete(request);

                return true;
            }


            File file = new File(fileParent, fileName);
            if (!file.exists()) {
                file.createNewFile();
            }

            connection.disconnect();

        } catch (Exception e) {
            DownLoadLog.d("read local file " + e.getMessage());
        }
        return false;

    }

    //截取包名后四位
    private String cutPackagename(Context mContext) {

        String cutP = mContext.getPackageName().replace(".", "");

        if (cutP != null && cutP.length() > 5) {
            return cutP.substring(cutP.length() - 5, cutP.length());
        }
        return cutP;

    }

    public String getFullFileName() {

        return localPath + File.separator + fileName;
    }


    /**
     * Called just before the thread finishes, regardless of status, to take any necessary action on
     * the downloaded file.
     */
    private void cleanUpDestination(DownloadRequest request) {
        DownLoadLog.d("cleanupDestination() deleting " + request.getUrl());
        File destinationFile = new File(request.getFileFullName());
        if (destinationFile.exists()) {
            destinationFile.delete();
        }
    }

    private void updateDownloadState(DownloadRequest request, int state) {
        request.setDownloadState(state);
    }

    private void updateDownloadComplete(DownloadRequest request) {
        mDelivery.postDownloadComplete(request);
        request.setDownloadState(DownloadManager.STATUS_SUCCESSFUL);
        //  request.finish();
    }

    private void updateDownloadFailed(DownloadRequest request, int errorCode, String errorMsg) {
        shouldAllowRedirects = false;
        request.setDownloadState(DownloadManager.STATUS_FAILED);
        if (request.getDeleteDestinationFileOnFailure()) {
            cleanUpDestination(request);
        }
        mDelivery.postDownloadFailed(request, errorCode, errorMsg);
        request.finish();
    }

    private void updateDownloadProgress(DownloadRequest request, int progress, long downloadedBytes) {
        mDelivery.postProgressUpdate(request, fileTotalSize, downloadedBytes, progress);
    }
}
