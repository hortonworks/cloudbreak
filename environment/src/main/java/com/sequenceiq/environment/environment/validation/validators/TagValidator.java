package com.sequenceiq.environment.environment.validation.validators;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Component
public class TagValidator {

    private final CloudPlatformConnectors cloudPlatformConnectors;

    public TagValidator(CloudPlatformConnectors cloudPlatformConnectors) {
        this.cloudPlatformConnectors = cloudPlatformConnectors;
    }

    public ValidationResult validateTags(String cloudPlatform, Map<String, String> tags) {
        return getTagValidatorConnector(cloudPlatform)
                .map(tagValidator -> tagValidator.validateTags(tags))
                .orElseGet(ValidationResult::empty);
    }

    public ValidationResult validateTagKeyLengths(String cloudPlatform, Collection<String> tagKeys) {
        return getTagValidatorConnector(cloudPlatform)
                .map(tagValidator -> tagValidator.validateTagKeyLengths(tagKeys))
                .orElseGet(ValidationResult::empty);
    }

    private Optional<com.sequenceiq.cloudbreak.cloud.TagValidator> getTagValidatorConnector(String cloudPlatform) {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform));
        return Optional.ofNullable(cloudPlatformConnectors.get(cloudPlatformVariant).parameters().tagValidator());
    }
}
