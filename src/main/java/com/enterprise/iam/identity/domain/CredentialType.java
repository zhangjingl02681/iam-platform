package com.enterprise.iam.identity.domain;

import com.enterprise.iam.common.jpa.CodedEnum;

public enum CredentialType implements CodedEnum {
    PASSWORD("password");

    private final String code;

    CredentialType(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
