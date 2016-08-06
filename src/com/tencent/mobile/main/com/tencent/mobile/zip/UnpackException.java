package com.tencent.mobile.main.com.tencent.mobile.zip;

/**
 * Created by realhe on 2016/8/6.
 */
public class UnpackException extends Exception{
    public UnpackException() {
    }

    public UnpackException(String message) {
        super(message);
    }

    public UnpackException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnpackException(Throwable cause) {
        super(cause);
    }

    public UnpackException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
