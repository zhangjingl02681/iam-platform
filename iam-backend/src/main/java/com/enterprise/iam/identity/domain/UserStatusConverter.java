package com.enterprise.iam.identity.domain;

import com.enterprise.iam.common.jpa.CodedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserStatusConverter extends CodedEnumConverter<UserStatus> {
    public UserStatusConverter() {
        super(UserStatus.class);
    }
}
