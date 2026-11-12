package com.enterprise.iam.oidc.domain;

import com.enterprise.iam.common.jpa.CodedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SigningKeyStatusConverter extends CodedEnumConverter<SigningKeyStatus> {
    public SigningKeyStatusConverter() {
        super(SigningKeyStatus.class);
    }
}
