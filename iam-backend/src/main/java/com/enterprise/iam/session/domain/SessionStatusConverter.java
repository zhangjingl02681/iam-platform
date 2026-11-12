package com.enterprise.iam.session.domain;

import com.enterprise.iam.common.jpa.CodedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SessionStatusConverter extends CodedEnumConverter<SessionStatus> {
    public SessionStatusConverter() {
        super(SessionStatus.class);
    }
}
