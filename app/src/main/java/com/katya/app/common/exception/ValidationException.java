package com.katya.app.common.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;


@Getter
public class ValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(message);
        this.fieldErrors = new HashMap<>();
    }

    public ValidationException(String field, String message) {
        super("Validation failed");
        this.fieldErrors = new HashMap<>();
        this.fieldErrors.put(field, message);
    }

    public ValidationException(Map<String, String> fieldErrors) {
        super("Multiple validation errors");
        this.fieldErrors = fieldErrors;
    }

    public void addFieldError(String field, String message) {
        this.fieldErrors.put(field, message);
    }
}