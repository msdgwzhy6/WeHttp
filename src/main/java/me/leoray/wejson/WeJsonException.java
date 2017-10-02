package me.leoray.wejson;

/**
 * JSON 序列化和反序列化异常
 */
public class WeJsonException extends Exception {
    public WeJsonException() {
    }

    public WeJsonException(String message) {
        super(message);
    }

    public WeJsonException(String message, Throwable cause) {
        super(message, cause);
    }

    public WeJsonException(Throwable cause) {
        super(cause);
    }

    public WeJsonException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
