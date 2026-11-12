package com.enterprise.iam.policy.domain;

import com.enterprise.iam.common.jpa.CodedEnum;

public enum PolicyLifecycleStatus implements CodedEnum {
    DRAFT("draft"),
    PUBLISHED("published"),
    DEPRECATED("deprecated");

    private final String code;

    PolicyLifecycleStatus(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
