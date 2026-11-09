package com.rad.center.service;

public class ValidationException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String ruleCode;

    public ValidationException(String ruleCode, String message) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public String getRuleCode() {
        return ruleCode;
    }
}

