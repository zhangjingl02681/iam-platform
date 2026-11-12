package com.enterprise.iam.oidc.web;

public record SigningKeyRotationResponse(SigningKeyResponse current, SigningKeyResponse previous) {}
