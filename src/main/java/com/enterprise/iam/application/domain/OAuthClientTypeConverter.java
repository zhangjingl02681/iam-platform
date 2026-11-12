package com.enterprise.iam.application.domain;

import com.enterprise.iam.common.jpa.CodedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OAuthClientTypeConverter extends CodedEnumConverter<OAuthClientType> {
    public OAuthClientTypeConverter() {
        super(OAuthClientType.class);
    }
}
