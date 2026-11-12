package com.enterprise.iam.pam.domain;

import com.enterprise.iam.common.jpa.CodedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PamRequestStatusConverter extends CodedEnumConverter<PamRequestStatus> {
    public PamRequestStatusConverter() {
        super(PamRequestStatus.class);
    }
}
