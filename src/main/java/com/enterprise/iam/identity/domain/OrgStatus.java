package com.enterprise.iam.identity.domain;

import com.enterprise.iam.common.jpa.CodedEnum;

public enum OrgStatus implements CodedEnum {
    ACTIVE("active"),
    DISABLED("disabled");

    private final String code;

    OrgStatus(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
