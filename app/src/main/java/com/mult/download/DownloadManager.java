package com.mult.download;

public interface DownloadManager {

    /**
     * Status when the download is currently pending.
     */
    int STATUS_PENDING = 1 << 0;

    /**
     * Status when the download is currently pending.
     */
    int STATUS_STARTED = 1 << 1;

    /**
     * Status when the download network call is connecting to destination.
     */
    int STATUS_CONNECTING = 1 << 2;

    /**
     * 正在下载
     * Status when the download is currently running.
     */
    int STATUS_RUNNING = 1 << 3;

    /**
     * 下载完成
     * Status when the download has successfully completed.
     */
    int STATUS_SUCCESSFUL = 1 << 4;

    /**
     * 下载失败
     * Status when the download has failed.
     */
    int STATUS_FAILED = 1 << 5;

    /**
     * 无效的下载地址/第一次下载
     * Status when the download has failed due to broken url or invalid download url
     */
    int STATUS_NOT_FOUND = 1 << 6;

    /**
     * 链接超时
     * Status when the download is attempted for retry due to connection timeouts.
     */
    int STATUS_RETRYING = 1 << 7;

    /**
     * Status when the download has paused
     */

    int STATUS_PAUSE = 1 << 8;

    /**
     * 目标文件/保存地址 有误
     * Error code when writing download content to the destination file.
     */
    int ERROR_FILE_ERROR = 1001;

    /**
     * Error code when an HTTP code was received that download manager can't
     * handle.
     */
    int ERROR_UNHANDLED_HTTP_CODE = 1002;

    /**
     * Error code when an error receiving or processing data occurred at the
     * HTTP level.
     */
    int ERROR_HTTP_DATA_ERROR = 1004;

    /**
     * Error code when there were too many redirects.
     */
    int ERROR_TOO_MANY_REDIRECTS = 1005;

    /**
     * Error code when size of the file is unknown.
     */
    int ERROR_DOWNLOAD_SIZE_UNKNOWN = 1006;

    /**
     * Error code when passed URI is malformed.
     */
    int ERROR_MALFORMED_URI = 1007;

    /**
     * Error code when download is cancelled.
     */
    int ERROR_DOWNLOAD_CANCELLED = 1008;

    /**
     * Error code when there is connection timeout after maximum retries
     */
    int ERROR_CONNECTION_TIMEOUT_AFTER_RETRIES = 1009;

    int add(DownloadRequest request);

    void resumeDownload(DownloadRequest downloadRequest,int downloadID);

    int pause(int downloadId);

    void pauseAll();

    int query(int downloadId);

    void release();

    boolean isReleased();

}
