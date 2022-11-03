package com.sequenceiq.redbeams.service.validation;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.TagValidator;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Component
public class RedBeamsTagValidator {

    private final CloudPlatformConnectors cloudPlatformConnectors;

    public RedBeamsTagValidator(CloudPlatformConnectors cloudPlatformConnectors) {
        this.cloudPlatformConnectors = cloudPlatformConnectors;
    }

    public ValidationResult validateTags(String cloudPlatform, Map<String, String> tags) {
        Optional<TagValidator> tagValidatorConnector = getTagValidatorConnector(cloudPlatform);
        if (tagValidatorConnector.isEmpty()) {
            return ValidationResult.empty();
        }
        return tagValidatorConnector.get().validateTags(tags);
    }

    private Optional<com.sequenceiq.cloudbreak.cloud.TagValidator> getTagValidatorConnector(String cloudPlatform) {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform));
        return Optional.ofNullable(cloudPlatformConnectors.get(cloudPlatformVariant).parameters().tagValidator());
    }

}
