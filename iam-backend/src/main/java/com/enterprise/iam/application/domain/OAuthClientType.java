package com.enterprise.iam.application.domain;

import com.enterprise.iam.common.jpa.CodedEnum;

public enum OAuthClientType implements CodedEnum {
    PUBLIC("public"),
    CONFIDENTIAL("confidential");

    private final String code;

    OAuthClientType(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
