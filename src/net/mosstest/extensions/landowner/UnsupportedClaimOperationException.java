package net.mosstest.extensions.landowner;

public class UnsupportedClaimOperationException extends LandClaimException {
    public UnsupportedClaimOperationException() {
        super();
    }

    public UnsupportedClaimOperationException(String message) {
        super(message);
    }

    public UnsupportedClaimOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedClaimOperationException(Throwable cause) {
        super(cause);
    }

    protected UnsupportedClaimOperationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
