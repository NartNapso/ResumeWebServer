package com.nart.aicv.exception;

public class ValidationException extends RuntimeException  {

    public ValidationException(String message) {
        super("Exception: " + message);
    }
}
