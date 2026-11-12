package com.enterprise.iam.pam.domain;

import com.enterprise.iam.common.jpa.CodedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PamLifecycleStatusConverter extends CodedEnumConverter<PamLifecycleStatus> {
    public PamLifecycleStatusConverter() {
        super(PamLifecycleStatus.class);
    }
}
