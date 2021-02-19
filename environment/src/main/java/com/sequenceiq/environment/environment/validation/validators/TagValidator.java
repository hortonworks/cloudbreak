package com.sequenceiq.environment.environment.validation.validators;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;

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
        //TODO gcp ignored due to special tag handling in gcp. remove this when cb factory tags (like Owner) sanitized in the env request
        if (GCP.name().equalsIgnoreCase(cloudPlatform)) {
            return ValidationResult.empty();
        }
        Optional<com.sequenceiq.cloudbreak.cloud.TagValidator> tagValidatorConnector = getTagValidatorConnector(cloudPlatform);
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
