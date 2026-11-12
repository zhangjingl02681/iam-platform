package com.enterprise.iam.application.domain;

import com.enterprise.iam.common.jpa.CodedEnum;

public enum ApplicationStatus implements CodedEnum {
    ACTIVE("active"),
    DISABLED("disabled");

    private final String code;

    ApplicationStatus(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
