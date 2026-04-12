package com.customagent.exception;

/** Thrown when an Agent cannot be found by its identifier. */
public class AgentNotFoundException extends RuntimeException {

    public AgentNotFoundException(String message) {
        super(message);
    }

    public AgentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
