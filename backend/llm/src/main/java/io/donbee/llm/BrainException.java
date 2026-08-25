package io.donbee.llm;

/** Thrown when a {@link Brain} fails to produce a response. */
public class BrainException extends RuntimeException {

    public BrainException(String message) {
        super(message);
    }

    public BrainException(String message, Throwable cause) {
        super(message, cause);
    }
}
