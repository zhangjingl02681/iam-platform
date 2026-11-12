package com.enterprise.iam.application.domain;

import com.enterprise.iam.common.jpa.CodedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ApplicationStatusConverter extends CodedEnumConverter<ApplicationStatus> {
    public ApplicationStatusConverter() {
        super(ApplicationStatus.class);
    }
}
