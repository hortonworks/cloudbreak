package com.sequenceiq.cloudbreak.cloud;

import java.util.Map;

import com.sequenceiq.cloudbreak.validation.ValidationResult;

public interface TagValidator {
    ValidationResult validateTags(Map<String, String> tags);
}
