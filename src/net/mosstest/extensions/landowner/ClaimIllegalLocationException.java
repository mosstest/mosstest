package net.mosstest.extensions.landowner;

/**
 * Thrown for claims in illegal areas (for example, for a claim framework not supporting claiming below ground level,
 * an attempt to add a claim below ground level will cause this to be thrown.
 * It should not be thrown for lookups in illegal areas.
 */
public class ClaimIllegalLocationException extends LandClaimException {
    public ClaimIllegalLocationException() {
        super();
    }

    public ClaimIllegalLocationException(String message) {
        super(message);
    }

    public ClaimIllegalLocationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClaimIllegalLocationException(Throwable cause) {
        super(cause);
    }

    protected ClaimIllegalLocationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
