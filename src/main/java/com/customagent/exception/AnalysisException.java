package com.customagent.exception;

/** Thrown for invalid analysis operations (report not found, illegal state transitions, etc.). */
public class AnalysisException extends RuntimeException {

    public AnalysisException(String message) {
        super(message);
    }

    public AnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
