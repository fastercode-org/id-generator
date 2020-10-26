package org.fastercode.idgenerator.spring.exception;

public class AutoConfigException extends RuntimeException {

    public AutoConfigException() {
    }

    public AutoConfigException(String message) {
        super(message);
    }

    public AutoConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public AutoConfigException(Throwable cause) {
        super(cause);
    }

    public AutoConfigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
