package com.enterprise.iam.session.domain;

import com.enterprise.iam.common.jpa.CodedEnum;

public enum SessionStatus implements CodedEnum {
    ACTIVE("active"),
    REVOKED("revoked"),
    EXPIRED("expired");

    private final String code;

    SessionStatus(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
