package com.enterprise.iam.pam.domain;

import com.enterprise.iam.common.jpa.CodedEnum;

public enum PamLifecycleStatus implements CodedEnum {
    ACTIVE("active"),
    DISABLED("disabled"),
    ROTATING("rotating");

    private final String code;

    PamLifecycleStatus(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
