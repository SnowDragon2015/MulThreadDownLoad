package com.mult.download;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadRequestQueue {

    /**
     * The set of all requests currently being processed by this RequestQueue. A Request will be in this set if it is waiting in any queue or currently being processed by any dispatcher.
     */
    private Set<DownloadRequest> mCurrentRequests = new HashSet<>();

    /**
     * The queue of requests that are actually going out to the network.
     */
    private PriorityBlockingQueue<DownloadRequest> mDownloadQueue = new PriorityBlockingQueue<>();

    /**
     * The download dispatchers
     */
    private DownloadThread[] mDownloadDispatchers;

    /**
     * Used for generating monotonically-increasing sequence numbers for requests.
     */
    private AtomicInteger mSequenceGenerator = new AtomicInteger();

    private CallBackDelivery mDelivery;

    /**
     * Delivery class to delivery the call back to call back registrar in main thread.
     */
    class CallBackDelivery {

        /**
         * Used for posting responses, typically to the main thread.
         */
        private final Executor mCallBackExecutor;

        /**
         * Constructor taking a handler to main thread.
         */
        public CallBackDelivery(final Handler handler) {
            // Make an Executor that just wraps the handler.
            mCallBackExecutor = new Executor() {
                @Override
                public void execute(Runnable command) {
                    handler.post(command);
                }
            };
        }

        public void postDownloadComplete(final DownloadRequest request) {
            mCallBackExecutor.execute(new Runnable() {
                public void run() {
                    if (request.getStatusListener() != null) {
                        request.getStatusListener().onDownloadComplete(request);
                    }
                }
            });
        }

        public void postDownloadFailed(final DownloadRequest request, final int errorCode, final String errorMsg) {
            mCallBackExecutor.execute(new Runnable() {
                public void run() {

                    if (request.getStatusListener() != null) {
                        request.getStatusListener().onDownloadFailed(request, errorCode, errorMsg);
                    }
                }
            });
        }

        public void postProgressUpdate(final DownloadRequest request, final long totalBytes, final long downloadedBytes, final int progress) {
            mCallBackExecutor.execute(new Runnable() {
                public void run() {
                    if (request.getStatusListener() != null) {
                        request.getStatusListener().onProgress(request, totalBytes, downloadedBytes, progress);
                    }
                }
            });
        }
    }

    /**
     * Default constructor.
     */
    public DownloadRequestQueue() {
        initialize(new Handler(Looper.getMainLooper()));
    }

    /**
     * Creates the download dispatchers workers pool.
     * <p>
     * Deprecated:
     */
    public DownloadRequestQueue(int threadPoolSize) {
        initialize(new Handler(Looper.getMainLooper()), threadPoolSize);
    }

    /**
     * Construct with provided callback handler.
     *
     * @param callbackHandler
     */
    public DownloadRequestQueue(Handler callbackHandler) throws InvalidParameterException {
        if (callbackHandler == null) {
            throw new InvalidParameterException("callbackHandler must not be null");
        }

        initialize(callbackHandler);
    }

    public void start() {
        stop(); // Make sure any currently running dispatchers are stopped.
        Log.i("chyy", "DownLoadRequestQueuw start mDownloadDispatchers "+mDownloadDispatchers.length);
        // Create download dispatchers (and corresponding threads) up to the pool size.
        for (int i = 0; i < mDownloadDispatchers.length; i++) {
            DownloadThread downloadDispatcher = new DownloadThread(mDownloadQueue, mDelivery);
            mDownloadDispatchers[i] = downloadDispatcher;
            Log.i("chyy","Start() -- i: "+i);
            downloadDispatcher.start();
        }
    }

    // Package-Private methods.

    /**
     * Generates a download id for the request and adds the download request to the download request queue for the dispatchers pool to act on immediately.
     *
     * @param request
     * @return downloadId
     */
    int add(DownloadRequest request) {
        Log.i("chyy", "DownLoadRequestQueuw add .. ");


        int downloadId = getDownloadId();
        // Tag the request as belonging to this queue and add it to the set of current requests.
        request.setDownloadRequestQueue(this);

        synchronized (mCurrentRequests) {
            mCurrentRequests.add(request);
        }

        // Process requests in the order they are added.
        request.setDownloadId(downloadId);
        mDownloadQueue.add(request);
;

        return downloadId;
    }

    int resumeDownload(DownloadRequest request, int downLoadId) {

        request.setDownloadRequestQueue(this);

        synchronized (mCurrentRequests) {
            mCurrentRequests.add(request);
        }
        request.cancleQu();

        // Process requests in the order they are added.
        request.setDownloadId(downLoadId);
        mDownloadQueue.add(request);

        return downLoadId;

    }

    /**
     * Returns the current download state for a download request.
     *
     * @param downloadId
     * @return
     */
    int query(int downloadId) {
        synchronized (mCurrentRequests) {
            for (DownloadRequest request : mCurrentRequests) {
                if (request.getDownloadId() == downloadId) {
                    return request.getDownloadState();
                }
            }
        }
        return DownloadManager.STATUS_NOT_FOUND;
    }

    /**
     * Cancel all the dispatchers in work and also stops the dispatchers.
     */
    void pauseAll() {

        synchronized (mCurrentRequests) {
            for (DownloadRequest request : mCurrentRequests) {
                request.cancel();
            }

            // Remove all the requests from the queue.
            mCurrentRequests.clear();
        }
    }



    /**
     * Cancel a particular download in progress. Returns 1 if the download Id is found else returns 0.
     *
     * @param downloadId
     * @return int
     */
    int pause(int downloadId) {
        synchronized (mCurrentRequests) {
            for (DownloadRequest request : mCurrentRequests) {
                if (request.getDownloadId() == downloadId) {

                    request.setDownloadState(DownloadManager.STATUS_PAUSE);
                    request.cancel();
                    return 1;
                }
            }
        }

        return 0;
    }

    void finish(DownloadRequest request) {
        if (mCurrentRequests != null) {//if finish and release are called together it throws NPE
            // Remove from the queue.
            synchronized (mCurrentRequests) {
                mCurrentRequests.remove(request);
            }
        }
    }

    /**
     * Cancels all the pending & running requests and releases all the dispatchers.
     */
    void release() {
        Log.i("chyy", "DownLoadRequestQueuw  release() ");
        if (mCurrentRequests != null) {
            synchronized (mCurrentRequests) {
                mCurrentRequests.clear();
                mCurrentRequests = null;
            }
        }

        if (mDownloadQueue != null) {
            mDownloadQueue = null;
        }

        if (mDownloadDispatchers != null) {
            stop();

            for (int i = 0; i < mDownloadDispatchers.length; i++) {
                mDownloadDispatchers[i] = null;
            }
            mDownloadDispatchers = null;
        }

    }

    // Private methods.

    /**
     * Perform construction.
     *
     * @param callbackHandler
     */
    private void initialize(Handler callbackHandler) {
        int processors = Runtime.getRuntime().availableProcessors();
        mDownloadDispatchers = new DownloadThread[processors];
        mDelivery = new CallBackDelivery(callbackHandler);
    }

    /**
     * Perform construction with custom thread pool size.
     */
    private void initialize(Handler callbackHandler, int threadPoolSize) {
        Log.i("chyy", "DownLoadRequestQueuw  initialize ");
        mDownloadDispatchers = new DownloadThread[threadPoolSize];
        mDelivery = new CallBackDelivery(callbackHandler);
    }

    /**
     * Stops download dispatchers.
     */
    private void stop() {
        Log.i("chyy", "DownLoadRequestQueuw  stop() ");
        for (int i = 0; i < mDownloadDispatchers.length; i++) {
            if (mDownloadDispatchers[i] != null) {
                mDownloadDispatchers[i].quit();
            }
        }
    }

    /**
     * Gets a sequence number.
     */
    private int getDownloadId() {
        return mSequenceGenerator.incrementAndGet();
    }
}
