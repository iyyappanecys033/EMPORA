package com.examly.springapp.exception;

public class UnauthorizedScopeException extends RuntimeException {
    public UnauthorizedScopeException(String message) {
        super(message);
    }
}

