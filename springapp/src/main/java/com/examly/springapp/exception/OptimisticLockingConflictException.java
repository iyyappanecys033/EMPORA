package com.examly.springapp.exception;

public class OptimisticLockingConflictException extends RuntimeException {
    public OptimisticLockingConflictException(String message) {
        super(message);
    }
}

