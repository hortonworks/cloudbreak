package com.sequenceiq.cloudbreak.tag;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.TagKeyNormalizer;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@Service
public class UserDefinedTagValidator {

    private static final String AWS_RESERVED_TAG_KEY_PREFIX = "aws:";

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

    public ValidationResult validateTagKeysToRemove(Collection<String> tagKeys, Map<String, String> defaultTags, Map<String, String> applicationTags,
            String cloudPlatform, TagKeyNormalizer tagKeyNormalizer) {
        ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        if (CollectionUtils.isEmpty(tagKeys)) {
            validationResultBuilder.error("Tag keys to remove must not be empty.");
        } else {
            // Only AWS needs this: its system tags (e.g. aws:cloudformation:*) are present on our resources but cannot be deleted, and a single
            // rejected key fails the whole DeleteTags call. Azure and GCP deletes are read-modify-write, so absent or reserved keys are a no-op.
            if (CloudPlatform.AWS.equalsIgnoreCase(cloudPlatform)) {
                validateAgainstReservedTagKeys(tagKeys, validationResultBuilder);
            }
            Set<String> defaultTagConflicts = tagKeys.stream()
                    .filter(key -> matchesProtectedTagKey(key, defaultTags, tagKeyNormalizer))
                    .collect(Collectors.toSet());
            if (!defaultTagConflicts.isEmpty()) {
                validationResultBuilder.error(buildProtectedTagKeyMessage("default", defaultTagConflicts));
            }
            Set<String> applicationTagConflicts = tagKeys.stream()
                    .filter(key -> matchesProtectedTagKey(key, applicationTags, tagKeyNormalizer))
                    .collect(Collectors.toSet());
            if (!applicationTagConflicts.isEmpty()) {
                validationResultBuilder.error(buildProtectedTagKeyMessage("application", applicationTagConflicts));
            }
        }
        return validationResultBuilder.build();
    }

    private static void validateAgainstReservedTagKeys(Collection<String> tagKeys, ValidationResultBuilder validationResultBuilder) {
        Set<String> reservedKeys = tagKeys.stream()
                .filter(key -> Strings.CI.startsWith(key, AWS_RESERVED_TAG_KEY_PREFIX))
                .collect(Collectors.toSet());
        if (!reservedKeys.isEmpty()) {
            validationResultBuilder.error(String.format("Tag key(s) %s cannot be removed because the '%s' prefix is reserved by AWS.",
                    reservedKeys, AWS_RESERVED_TAG_KEY_PREFIX));
        }
    }

    private boolean matchesProtectedTagKey(String requestedKey, Map<String, String> protectedTags, TagKeyNormalizer tagKeyNormalizer) {
        if (CollectionUtils.isEmpty(protectedTags)) {
            return false;
        }
        String normalizedRequestedKey = tagKeyNormalizer.normalize(requestedKey);
        return protectedTags.keySet().stream()
                .anyMatch(protectedKey -> tagKeyNormalizer.normalize(protectedKey).equals(normalizedRequestedKey));
    }

    private String buildConflictMessage(Set<String> conflictingKeys) {
        return String.format(
                "User-defined tag key(s) %s conflict with default tag key(s). Default tags cannot be overridden by user-defined tags.",
                conflictingKeys);
    }

    private String buildProtectedTagKeyMessage(String tagType, Set<String> conflictingKeys) {
        return String.format(
                "Tag key(s) %s cannot be removed because they are %s tag key(s).",
                conflictingKeys,
                tagType);
    }
}
