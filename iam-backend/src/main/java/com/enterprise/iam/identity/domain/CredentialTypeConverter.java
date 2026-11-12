package com.enterprise.iam.identity.domain;

import com.enterprise.iam.common.jpa.CodedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CredentialTypeConverter extends CodedEnumConverter<CredentialType> {
    public CredentialTypeConverter() {
        super(CredentialType.class);
    }
}
