package com.enterprise.iam.identity.domain;

import com.enterprise.iam.common.jpa.CodedEnum;

public enum UserStatus implements CodedEnum {
    ACTIVE("active"),
    DISABLED("disabled"),
    LOCKED("locked"),
    TERMINATED("terminated");

    private final String code;

    UserStatus(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }

    public boolean isLoginAllowed() {
        return this == ACTIVE || this == LOCKED;
    }

    public boolean isTerminal() {
        return this == TERMINATED;
    }
}
