package com.enterprise.iam.identity.domain;

import com.enterprise.iam.common.jpa.CodedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ImportJobStatusConverter extends CodedEnumConverter<ImportJobStatus> {
    public ImportJobStatusConverter() {
        super(ImportJobStatus.class);
    }
}
