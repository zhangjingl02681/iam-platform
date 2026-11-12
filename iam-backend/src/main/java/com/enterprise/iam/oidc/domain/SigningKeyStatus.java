package com.enterprise.iam.oidc.domain;

import com.enterprise.iam.common.jpa.CodedEnum;

public enum SigningKeyStatus implements CodedEnum {
    CURRENT("current"),
    PREVIOUS("previous"),
    RETIRED("retired");

    private final String code;

    SigningKeyStatus(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
