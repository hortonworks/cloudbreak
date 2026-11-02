package com.sequenceiq.cloudbreak.cloud;

import java.util.Collection;
import java.util.Map;

import com.sequenceiq.cloudbreak.validation.ValidationResult;

public interface TagValidator {
    ValidationResult validateTags(Map<String, String> tags);

    ValidationResult validateTagKeyLengths(Collection<String> tagKeys);
}
