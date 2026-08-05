package com.sequenceiq.cloudbreak.tag;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@Service
public class UserDefinedTagValidator {

    public ValidationResult validateAgainstDefaultTags(Map<String, String> userDefinedTags, Map<String, String> defaultTags) {
        ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        if (CollectionUtils.isEmpty(userDefinedTags) || CollectionUtils.isEmpty(defaultTags)) {
            return validationResultBuilder.build();
        }
        Set<String> conflictingKeys = userDefinedTags.keySet().stream()
                .filter(defaultTags::containsKey)
                .collect(Collectors.toSet());
        if (!conflictingKeys.isEmpty()) {
            validationResultBuilder.error(buildConflictMessage(conflictingKeys));
        }
        return validationResultBuilder.build();
    }

    private String buildConflictMessage(Set<String> conflictingKeys) {
        return String.format(
                "User-defined tag key(s) %s conflict with default tag key(s). Default tags cannot be overridden by user-defined tags.",
                conflictingKeys);
    }
}
