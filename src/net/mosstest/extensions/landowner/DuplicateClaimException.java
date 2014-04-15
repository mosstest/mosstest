package net.mosstest.extensions.landowner;

public class DuplicateClaimException extends LandClaimException {
    public DuplicateClaimException() {
        super();
    }

    public DuplicateClaimException(String message) {
        super(message);
    }

    public DuplicateClaimException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateClaimException(Throwable cause) {
        super(cause);
    }

    protected DuplicateClaimException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
