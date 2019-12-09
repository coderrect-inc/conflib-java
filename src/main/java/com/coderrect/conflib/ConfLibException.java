package com.coderrect.conflib;

public class ConfLibException extends RuntimeException {
    public ConfLibException(Throwable cause) {
        super(cause);
    }

    public ConfLibException(String msg) {
        super(msg);
    }
}
