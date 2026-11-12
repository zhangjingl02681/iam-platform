package com.enterprise.iam.common.exception;

public class NotFoundException extends RuntimeException {

    private final String code;

    public NotFoundException(String message) {
        this("not_found", message);
    }

    public NotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
