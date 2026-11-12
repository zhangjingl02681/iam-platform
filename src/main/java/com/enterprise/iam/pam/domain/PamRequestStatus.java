package com.enterprise.iam.pam.domain;

import com.enterprise.iam.common.jpa.CodedEnum;

public enum PamRequestStatus implements CodedEnum {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    CANCELLED("cancelled"),
    EXPIRED("expired");

    private final String code;

    PamRequestStatus(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
