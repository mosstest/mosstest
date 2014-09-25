package net.mosstest.servercore;

/**
 * Created by hexafraction on 9/25/14.
 */
public class BadConnectionStateException extends RuntimeException {
    public BadConnectionStateException(String message) {
    }

    public BadConnectionStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadConnectionStateException(Throwable cause) {
        super(cause);
    }

    public BadConnectionStateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public BadConnectionStateException() {
    }
}

