package com.enterprise.iam.identity.domain;

import com.enterprise.iam.common.jpa.CodedEnum;

public enum ImportJobStatus implements CodedEnum {
    PENDING("pending"),
    RUNNING("running"),
    SUCCEEDED("succeeded"),
    FAILED("failed"),
    CANCELLED("cancelled");

    private final String code;

    ImportJobStatus(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
