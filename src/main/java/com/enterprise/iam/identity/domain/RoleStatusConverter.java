package com.enterprise.iam.identity.domain;

import com.enterprise.iam.common.jpa.CodedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleStatusConverter extends CodedEnumConverter<RoleStatus> {
    public RoleStatusConverter() {
        super(RoleStatus.class);
    }
}
