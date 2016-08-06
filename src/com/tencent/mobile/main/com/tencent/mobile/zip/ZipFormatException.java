package com.tencent.mobile.main.com.tencent.mobile.zip;

/**
 * Created by realhe on 2016/7/22.
 */
public class ZipFormatException extends Exception {
    public ZipFormatException() {
    }

    public ZipFormatException(String message) {
        super(message);
    }

    public ZipFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZipFormatException(Throwable cause) {
        super(cause);
    }

    public ZipFormatException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
