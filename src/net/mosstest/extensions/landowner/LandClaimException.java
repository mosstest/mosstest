package net.mosstest.extensions.landowner;

/**
 * Base class for all exceptions that may occur with land claims.
 */
public class LandClaimException extends Exception {
    public LandClaimException() {
        super();
    }

    public LandClaimException(String message) {
        super(message);
    }

    public LandClaimException(String message, Throwable cause) {
        super(message, cause);
    }

    public LandClaimException(Throwable cause) {
        super(cause);
    }

    protected LandClaimException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
