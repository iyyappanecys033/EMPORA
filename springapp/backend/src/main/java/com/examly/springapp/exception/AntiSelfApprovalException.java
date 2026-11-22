package com.examly.springapp.exception;

public class AntiSelfApprovalException extends BusinessRuleViolationException {
    public AntiSelfApprovalException(String message) {
        super(message);
    }
}

