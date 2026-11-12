package com.enterprise.iam.identity.web.dto;

import java.util.List;

public record MeResponse(
        UserResponse profile, boolean mustChangePassword, boolean admin, List<String> authorities) {}
