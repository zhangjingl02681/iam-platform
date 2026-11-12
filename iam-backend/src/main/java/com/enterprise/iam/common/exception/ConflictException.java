package com.enterprise.iam.common.exception;

public class ConflictException extends RuntimeException {

    private final String code;

    public ConflictException(String message) {
        this("conflict", message);
    }

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
