package com.mult.download;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //http://192.168.21.100:8080/UploadFile/image/cyq.jpg

    //private static final String FILE1 = "http://54.223.56.161/app/Lantern1.4.apk";

    private static final String FILE1 = "http://192.168.21.100:8080/UploadFile/image/video2.avi";

    private static final String FILE2 = "http://imgstore.cdn.sogou.com/app/a/100540002/908494.jpg";
    private static final String FILE3 = "http://54.223.56.161/app/万能记事本4.2.10.apk";
   // private static final String FILE4 = "http://54.223.56.161/app/com.bookstore.txtreader.s-v1.1.83-v50-6c719c52bc_1183_1_vivo_sign.apk";

    private static final String FILE4 = "http://192.168.21.100:8080/UploadFile/image/video2.mp4";
    private static final String FILE5 = "http://httpbin.org/headers";
    private static final String FILE6 = "http://imgstore.cdn.sogou.com/app/a/100540002/470357.jpg";

    private ThinDownloadManager downloadManager;
    private static final int DOWNLOAD_THREAD_POOL_SIZE =4;

    Button mDownload1;
    Button mDownload2;
    Button mDownload3;
    Button mDownload4;
    Button mDownload5;

    Button pauseDwonLoad1;
    Button pauseDwonLoad2;
    Button pauseDwonLoad3;
    Button pauseDwonLoad4;

    Button mStartAll;
    Button mCancelAll;
    Button mListFiles;

    ProgressBar mProgress1;
    ProgressBar mProgress2;
    ProgressBar mProgress3;
    ProgressBar mProgress4;
    ProgressBar mProgress5;

    TextView mProgress1Txt;
    TextView mProgress2Txt;
    TextView mProgress3Txt;
    TextView mProgress4Txt;
    TextView mProgress5Txt;


    DownloadRequest downloadRequest1;
    DownloadRequest downloadRequest2;
    DownloadRequest downloadRequest3;
    DownloadRequest downloadRequest4;
    DownloadRequest downloadRequest5;
    DownloadRequest downloadRequest6;


    MyDownloadDownloadStatusListenerV1
            myDownloadStatusListener = new MyDownloadDownloadStatusListenerV1();

    int downloadId1;
    int downloadId2;
    int downloadId3;
    int downloadId4;
    int downloadId5;
    int downloadId6;

    RetryPolicy retryPolicy;
    private String localPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        localPath = getCacheDir().getPath();

        initWidget();

        downloadManager = new ThinDownloadManager(DOWNLOAD_THREAD_POOL_SIZE);
        retryPolicy = new DefaultRetryPolicy();

        initDownLoadRequest();


        mListFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInternalFilesDir();
            }
        });


    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button1:
                // Log.i("chyy", "onClick Button1  " + "download ID :: " + downloadId1 + " query status -- " + downloadManager.query(downloadId1));
                int i = downloadManager.query(downloadId1);
                if (i == DownloadManager.STATUS_STARTED) {//开始下载

                } else if (i == DownloadManager.STATUS_NOT_FOUND) {
                    downloadId1 = downloadManager.add(downloadRequest1);

                } else if (i == DownloadManager.STATUS_RUNNING) {//正在下载
                    downloadManager.resumeDownload(downloadRequest1, downloadId1);

                } else if (i == DownloadManager.STATUS_FAILED) {//下载失败

                } else if (i == DownloadManager.STATUS_PAUSE) {//暂停下载
                    downloadManager.resumeDownload(downloadRequest1, downloadId1);


                } else if (i == DownloadManager.STATUS_SUCCESSFUL) {//下载完成

                }

                   Log.i("chyy", ">>>>>>>>>>>>>onClick Button1  " + "download ID :: " + downloadId1 + " query status -- " + downloadManager.query(downloadId1));
                break;
            case R.id.button1_stop:
                downloadManager.pause(downloadId1);
                break;
            case R.id.button2:

                if (downloadManager.query(downloadId2) == DownloadManager.STATUS_NOT_FOUND) {
                    downloadId2 = downloadManager.add(downloadRequest2);
                }else if (downloadManager.query(downloadId2) == DownloadManager.STATUS_PAUSE){
                    downloadManager.resumeDownload(downloadRequest2,downloadId2);
                }
                Log.i("chyy", ">>>>>>>>>>>>>onClick Button2  " + "download ID :: " + downloadId2 + " query status -- " + downloadManager.query(downloadId2));
                break;
            case R.id.button2_stop:
                downloadManager.pause(downloadId2);
                break;
            case R.id.button3:


                if (downloadManager.query(downloadId3) == DownloadManager.STATUS_NOT_FOUND) {
                    downloadId3 = downloadManager.add(downloadRequest3);
                } else if (downloadManager.query(downloadId3) == DownloadManager.STATUS_PAUSE) {
                    downloadManager.resumeDownload(downloadRequest3,downloadId3);
                }
                Log.i("chyy", ">>>>>>>>>>>>>onClick Button3  " + "download ID :: " + downloadId3 + " query status -- " + downloadManager.query(downloadId3));

                break;
            case R.id.button3_stop:
                downloadManager.pause(downloadId3);
                break;
            case R.id.button4:

                if (downloadManager.query(downloadId4) == DownloadManager.STATUS_NOT_FOUND) {
                    downloadId4 = downloadManager.add(downloadRequest4);
                }else if (downloadManager.query(downloadId4) == DownloadManager.STATUS_PAUSE){
                    downloadManager.resumeDownload(downloadRequest4,downloadId4);
                }
                Log.i("chyy", ">>>>>>>>>>>>>onClick Button4  " + "download ID :: " + downloadId4 + " query status -- " + downloadManager.query(downloadId4));
                break;
            case R.id.button4_stop:
                downloadManager.pause(downloadId4);
                break;
            case R.id.button_download_headers:

                //if (downloadManager.query(downloadId5) == DownloadManager.STATUS_NOT_FOUND) {
                //    downloadId5 = downloadManager.add(downloadRequest5);
                //}

                if (downloadManager.query(downloadId6) == DownloadManager.STATUS_NOT_FOUND) {
                    downloadId6 = downloadManager.add(downloadRequest6);
                }
                break;
            case R.id.button5:
                downloadManager.pauseAll();
//                downloadId1 = downloadManager.add(downloadRequest1);
//                downloadId2 = downloadManager.add(downloadRequest2);
//                downloadId3 = downloadManager.add(downloadRequest3);
//                downloadId4 = downloadManager.add(downloadRequest4);
                // downloadId5 = downloadManager.add(downloadRequest5);
                break;
            case R.id.button6:
                downloadManager.pauseAll();
                break;


        }

    }

    private void initWidget() {

        mDownload1 = (Button) findViewById(R.id.button1);
        mDownload2 = (Button) findViewById(R.id.button2);
        mDownload3 = (Button) findViewById(R.id.button3);
        mDownload4 = (Button) findViewById(R.id.button4);
        mDownload5 = (Button) findViewById(R.id.button_download_headers);

        mDownload1.setOnClickListener(this);
        mDownload2.setOnClickListener(this);
        mDownload3.setOnClickListener(this);
        mDownload4.setOnClickListener(this);
        mDownload5.setOnClickListener(this);

        pauseDwonLoad1 = (Button) findViewById(R.id.button1_stop);
        pauseDwonLoad2 = (Button) findViewById(R.id.button2_stop);
        pauseDwonLoad3 = (Button) findViewById(R.id.button3_stop);
        pauseDwonLoad4 = (Button) findViewById(R.id.button4_stop);

        pauseDwonLoad1.setOnClickListener(this);
        pauseDwonLoad2.setOnClickListener(this);
        pauseDwonLoad3.setOnClickListener(this);
        pauseDwonLoad4.setOnClickListener(this);

        mStartAll = (Button) findViewById(R.id.button5);
        mCancelAll = (Button) findViewById(R.id.button6);
        mListFiles = (Button) findViewById(R.id.button7);

        mStartAll.setOnClickListener(this);
        mCancelAll.setOnClickListener(this);
        mListFiles.setOnClickListener(this);

        mProgress1Txt = (TextView) findViewById(R.id.progressTxt1);
        mProgress2Txt = (TextView) findViewById(R.id.progressTxt2);
        mProgress3Txt = (TextView) findViewById(R.id.progressTxt3);
        mProgress4Txt = (TextView) findViewById(R.id.progressTxt4);
        mProgress5Txt = (TextView) findViewById(R.id.progressTxt5);

        mProgress1 = (ProgressBar) findViewById(R.id.progress1);
        mProgress2 = (ProgressBar) findViewById(R.id.progress2);
        mProgress3 = (ProgressBar) findViewById(R.id.progress3);
        mProgress4 = (ProgressBar) findViewById(R.id.progress4);
        mProgress5 = (ProgressBar) findViewById(R.id.progress5);

        mProgress1.setMax(100);
        mProgress1.setProgress(0);

        mProgress2.setMax(100);
        mProgress2.setProgress(0);

        mProgress3.setMax(100);
        mProgress3.setProgress(0);

        mProgress4.setMax(100);
        mProgress4.setProgress(0);

        mProgress5.setMax(100);
        mProgress5.setProgress(0);

        mProgress1Txt.setText("Download1");
        mProgress2Txt.setText("Download2");
        mProgress3Txt.setText("Download3");
        mProgress4Txt.setText("Download4");
        mProgress5Txt.setText("Download5");

    }

    private void initDownLoadRequest() {


        downloadRequest1 = new DownloadRequest(this)
                .setDownLoadUrl(FILE1)
                .setLocalPath(localPath)
                .setPriority(DownloadRequest.Priority.LOW)
                .setRetryPolicy(retryPolicy)
                .setDownloadContext("Download1")
                .setStatusListener(myDownloadStatusListener);

        downloadRequest2 = new DownloadRequest(this)
                .setDownLoadUrl(FILE2)
                .setPriority(DownloadRequest.Priority.LOW)
                .setDownloadContext("Download2")
                .setStatusListener(myDownloadStatusListener);


        downloadRequest3 = new DownloadRequest(this)
                .setDownLoadUrl(FILE3)
                .setLocalPath(localPath).setPriority(DownloadRequest.Priority.HIGH)
                .setDownloadContext("Download3")
                .setStatusListener(myDownloadStatusListener);


        // Define a custom retry policy
        retryPolicy = new DefaultRetryPolicy(5000, 3, 2f);
        downloadRequest4 = new DownloadRequest(this)
                .setDownLoadUrl(FILE4)
                .setRetryPolicy(retryPolicy)
                .setLocalPath(localPath).setPriority(DownloadRequest.Priority.HIGH)
                .setDownloadContext("Download4")
                .setStatusListener(myDownloadStatusListener);


        downloadRequest5 = new DownloadRequest(this)
                .setDownLoadUrl(FILE5)
                .addCustomHeader("Auth-Token", "myTokenKey")
                .addCustomHeader("User-Agent", "Thin/Android")
                .setLocalPath(localPath).setPriority(DownloadRequest.Priority.HIGH)
                .setDownloadContext("Download5")
                .setStatusListener(myDownloadStatusListener);


        downloadRequest6 = new DownloadRequest(this)
                .setDownLoadUrl(FILE6)
                .setLocalPath(localPath).setPriority(DownloadRequest.Priority.HIGH)
                .setDownloadContext("Download6")
                .setStatusListener(myDownloadStatusListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("######## onDestroy ######## ");
        downloadManager.release();
    }

    private void showInternalFilesDir() {
        File internalFile = new File(getExternalFilesDir("").getPath());
        File files[] = internalFile.listFiles();
        String contentText = "";
        if (files.length == 0) {
            contentText = "No Files Found";
        }

        for (File file : files) {
            contentText += file.getName() + " " + file.length() + " \n\n ";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog internalCacheDialog = builder.create();
        LayoutInflater inflater = internalCacheDialog.getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.layout_files, null);
        TextView content = (TextView) dialogLayout.findViewById(R.id.filesList);
        content.setText(contentText);

        builder.setView(dialogLayout);
        builder.show();

    }


    class MyDownloadDownloadStatusListenerV1 implements DownloadStatusListener {

        @Override
        public void onDownloadComplete(DownloadRequest request) {
            final int id = request.getDownloadId();
            if (id == downloadId1) {
                mProgress1Txt.setText(request.getDownloadContext() + " id: " + id + " Completed");

            } else if (id == downloadId2) {
                mProgress2Txt.setText(request.getDownloadContext() + " id: " + id + " Completed");

            } else if (id == downloadId3) {
                mProgress3Txt.setText(request.getDownloadContext() + " id: " + id + " Completed");

            } else if (id == downloadId4) {
                mProgress4Txt.setText(request.getDownloadContext() + " id: " + id + " Completed");
            } else if (id == downloadId5) {
                mProgress5Txt.setText(request.getDownloadContext() + " id: " + id + " Completed");
            }
        }

        @Override
        public void onDownloadFailed(DownloadRequest request, int errorCode, String errorMessage) {
            final int id = request.getDownloadId();
            if (id == downloadId1) {
                mProgress1Txt.setText("Download1 id: " + id + " Failed: ErrorCode " + errorCode + ", " + errorMessage);
                mProgress1.setProgress(0);
            } else if (id == downloadId2) {
                mProgress2Txt.setText("Download2 id: " + id + " Failed: ErrorCode " + errorCode + ", " + errorMessage);
                mProgress2.setProgress(0);

            } else if (id == downloadId3) {
                mProgress3Txt.setText("Download3 id: " + id + " Failed: ErrorCode " + errorCode + ", " + errorMessage);
                mProgress3.setProgress(0);

            } else if (id == downloadId4) {
                mProgress4Txt.setText("Download4 id: " + id + " Failed: ErrorCode " + errorCode + ", " + errorMessage);
                mProgress4.setProgress(0);
            } else if (id == downloadId5) {
                mProgress5Txt.setText("Download5 id: " + id + " Failed: ErrorCode " + errorCode + ", " + errorMessage);
                mProgress5.setProgress(0);
            }
        }

        @Override
        public void onProgress(DownloadRequest request, long totalBytes, long downloadedBytes, int progress) {
            int id = request.getDownloadId();

      //      System.out.println("######## onProgress ###### " + id + " : " + totalBytes + " : " + downloadedBytes + " : " + progress);
            if (id == downloadId1) {
                mProgress1Txt.setText("Download1 id: " + id + ", " + progress + "%" + "  " + getBytesDownloaded(progress, totalBytes));
                mProgress1.setProgress(progress);

            } else if (id == downloadId2) {
                mProgress2Txt.setText("Download2 id: " + id + ", " + progress + "%" + "  " + getBytesDownloaded(progress, totalBytes));
                mProgress2.setProgress(progress);

            } else if (id == downloadId3) {
                mProgress3Txt.setText("Download3 id: " + id + ", " + progress + "%" + "  " + getBytesDownloaded(progress, totalBytes));
                mProgress3.setProgress(progress);

            } else if (id == downloadId4) {
                mProgress4Txt.setText("Download4 id: " + id + ", " + progress + "%" + "  " + getBytesDownloaded(progress, totalBytes));
                mProgress4.setProgress(progress);
            } else if (id == downloadId5) {
                mProgress5Txt.setText("Download5 id: " + id + ", " + progress + "%" + "  " + getBytesDownloaded(progress, totalBytes));
                mProgress5.setProgress(progress);
            } else if (id == downloadId6) {
                mProgress5Txt.setText("Download6 id: " + id + ", " + progress + "%" + "  " + getBytesDownloaded(progress, totalBytes));
                mProgress5.setProgress(progress);
            }
        }
    }

    private Uri geUri(File filesDir, String path) {

        String[] tempArr = path.split("/");
        String fileName = tempArr[tempArr.length - 1];
        return Uri.parse(filesDir + "/mani/" + fileName);
    }

    private String getBytesDownloaded(int progress, long totalBytes) {
        //Greater than 1 MB
        long bytesCompleted = (progress * totalBytes) / 100;
        if (totalBytes >= 1000000) {
            return ("" + (String.format("%.1f", (float) bytesCompleted / 1000000)) + "/" + (String.format("%.1f", (float) totalBytes / 1000000)) + "MB");
        }
        if (totalBytes >= 1000) {
            return ("" + (String.format("%.1f", (float) bytesCompleted / 1000)) + "/" + (String.format("%.1f", (float) totalBytes / 1000)) + "Kb");

        } else {
            return ("" + bytesCompleted + "/" + totalBytes);
        }
    }

}
