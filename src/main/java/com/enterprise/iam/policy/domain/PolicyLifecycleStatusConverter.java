package com.enterprise.iam.policy.domain;

import com.enterprise.iam.common.jpa.CodedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PolicyLifecycleStatusConverter extends CodedEnumConverter<PolicyLifecycleStatus> {
    public PolicyLifecycleStatusConverter() {
        super(PolicyLifecycleStatus.class);
    }
}
