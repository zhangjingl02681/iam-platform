package com.enterprise.iam.identity.domain;

import com.enterprise.iam.common.jpa.CodedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PositionStatusConverter extends CodedEnumConverter<PositionStatus> {
    public PositionStatusConverter() {
        super(PositionStatus.class);
    }
}
