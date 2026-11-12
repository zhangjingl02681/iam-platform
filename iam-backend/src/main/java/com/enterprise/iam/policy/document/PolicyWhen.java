package com.enterprise.iam.policy.document;

import java.util.List;
import java.util.Map;

public record PolicyWhen(List<String> roles, List<String> orgCodes, Map<String, String> attributes) {}
