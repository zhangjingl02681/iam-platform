package com.enterprise.iam.application.domain;

import com.enterprise.iam.common.jpa.CodedEnum;

public enum EntitlementSubjectType implements CodedEnum {
    USER("user"),
    ORG("org"),
    ROLE("role");

    private final String code;

    EntitlementSubjectType(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
