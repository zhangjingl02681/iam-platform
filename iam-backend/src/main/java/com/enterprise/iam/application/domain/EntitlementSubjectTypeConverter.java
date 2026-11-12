package com.enterprise.iam.application.domain;

import com.enterprise.iam.common.jpa.CodedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EntitlementSubjectTypeConverter extends CodedEnumConverter<EntitlementSubjectType> {
    public EntitlementSubjectTypeConverter() {
        super(EntitlementSubjectType.class);
    }
}
