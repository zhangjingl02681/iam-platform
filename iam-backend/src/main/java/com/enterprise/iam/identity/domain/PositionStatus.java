package com.enterprise.iam.identity.domain;

import com.enterprise.iam.common.jpa.CodedEnum;

public enum PositionStatus implements CodedEnum {
    ACTIVE("active"),
    DISABLED("disabled");

    private final String code;

    PositionStatus(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
