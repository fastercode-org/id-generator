package org.fastercode.idgenerator.core.exception;

public class IDGeneratorException extends RuntimeException {
    private static final long serialVersionUID = 8640369996444196488L;

    public IDGeneratorException() {
        super();
    }

    public IDGeneratorException(String msg) {
        super(msg);
    }

    public IDGeneratorException(String message, Throwable cause) {
        super(message, cause);
    }
}
