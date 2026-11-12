package com.enterprise.iam.application.domain;

import com.enterprise.iam.common.jpa.CodedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OAuthClientStatusConverter extends CodedEnumConverter<OAuthClientStatus> {
    public OAuthClientStatusConverter() {
        super(OAuthClientStatus.class);
    }
}
