package org.fastercode.idgenerator.core.exception;

public class WorkerIDCanNotGenAnyMore extends RuntimeException {

    private static final long serialVersionUID = -6086464028436244206L;

    public WorkerIDCanNotGenAnyMore() {
        super();
    }

    public WorkerIDCanNotGenAnyMore(String msg) {
        super(msg);
    }

    public WorkerIDCanNotGenAnyMore(String message, Throwable cause) {
        super(message, cause);
    }
}
