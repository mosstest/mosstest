package net.mosstest.netcommand;

/**
 * The Class MalformedPacketException.
 */
public class MalformedPacketException extends Exception {
    public MalformedPacketException() {
    }

    public MalformedPacketException(String message) {
        super(message);
    }

    public MalformedPacketException(String message, Throwable cause) {
        super(message, cause);
    }

    public MalformedPacketException(Throwable cause) {
        super(cause);
    }

    public MalformedPacketException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
