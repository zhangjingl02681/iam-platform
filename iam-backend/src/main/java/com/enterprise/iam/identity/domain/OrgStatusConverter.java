package com.enterprise.iam.identity.domain;

import com.enterprise.iam.common.jpa.CodedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OrgStatusConverter extends CodedEnumConverter<OrgStatus> {
    public OrgStatusConverter() {
        super(OrgStatus.class);
    }
}
