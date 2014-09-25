package net.mosstest.scripting.authentication;

/**
 * Created by hexafraction on 9/24/14.
 */
public class AccessDenied extends Exception {
    public final DenialReason reason;

    public AccessDenied(DenialReason reason) {
        this.reason = reason;
    }

    public AccessDenied(String message, DenialReason reason) {
        super(message);
        this.reason = reason;
    }

    public AccessDenied(String message, Throwable cause, DenialReason reason) {
        super(message, cause);
        this.reason = reason;
    }

    public AccessDenied(Throwable cause, DenialReason reason) {
        super(cause);
        this.reason = reason;
    }

    public AccessDenied(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, DenialReason reason) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.reason = reason;
    }
}
