package com.enterprise.iam.identity.web.dto;

import java.util.List;

public record MfaConfirmResponse(MfaFactorResponse factor, List<String> recoveryCodes) {}
