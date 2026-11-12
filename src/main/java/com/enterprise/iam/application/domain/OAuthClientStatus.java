package com.enterprise.iam.application.domain;

import com.enterprise.iam.common.jpa.CodedEnum;

public enum OAuthClientStatus implements CodedEnum {
    ACTIVE("active"),
    DISABLED("disabled");

    private final String code;

    OAuthClientStatus(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
