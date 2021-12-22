package com.sequenceiq.cloudbreak.controller.validation.dr;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.MOCK;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Component
public class BackupRestoreV4RequestValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupRestoreV4RequestValidator.class);

    private static final String[] SUPPORTED_CLOUD_PLATFORMS = {AWS, AZURE, GCP, MOCK};

    private static final Map<String, Pattern> CLOUD_PLATFORM_TO_PATTERN = Map.of(
            AWS, Pattern.compile("^s3[a|n]?$", Pattern.CASE_INSENSITIVE),
            AZURE, Pattern.compile("^abfs[s]?$", Pattern.CASE_INSENSITIVE),
            GCP, Pattern.compile("^gs$", Pattern.CASE_INSENSITIVE),
            MOCK, Pattern.compile("^mock$", Pattern.CASE_INSENSITIVE)
    );

    public ValidationResult validate(Stack stack, String location, String backupId) {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        String cloudPlatform = stack.cloudPlatform();

        String validCloudPlatform = validateCloudPlatformAndReturn(cloudPlatform, resultBuilder);
        if (Strings.isNullOrEmpty(backupId)) {
            resultBuilder.error("Parameter backupId required");
        }
        if (Strings.isNullOrEmpty(location)) {
            resultBuilder.error("Parameter backupLocation required");
        } else {
            validateCloudLocationScheme(validCloudPlatform, location, resultBuilder);
        }
        return resultBuilder.build();
    }

    private String validateCloudPlatformAndReturn(String cloudPlatform, ValidationResult.ValidationResultBuilder resultBuilder) {
        String validCloudPlatform = Arrays.stream(SUPPORTED_CLOUD_PLATFORMS).filter(cloudPlatform::equalsIgnoreCase)
                .findFirst().orElse(null);

        if (validCloudPlatform == null) {
            resultBuilder.error("Cloud platform \"" + cloudPlatform + "\" not supported for backup/restore");
        }

        return validCloudPlatform;
    }

    private void validateCloudLocationScheme(String validCloudPlatform, String location, ValidationResult.ValidationResultBuilder resultBuilder) {
        try {
            URI uri = new URI(location);
            if (Strings.isNullOrEmpty(uri.getScheme())) {
                LOGGER.warn("Using a null or empty URI scheme for backup location");
                return;
            }
            if (uri.getScheme().equalsIgnoreCase("hdfs")) {
                return;
            }
            if (validCloudPlatform != null) {
                Matcher matcher = CLOUD_PLATFORM_TO_PATTERN.get(validCloudPlatform).matcher(uri.getScheme());
                if (!matcher.find()) {
                    resultBuilder.error("Incorrect scheme \"" + uri.getScheme() + "\" for cloud platform \"" + validCloudPlatform + "\"");
                }
            } else {
                resultBuilder.error("Unsupported cloud platform, can't validate scheme");
            }
        } catch (URISyntaxException e) {
            resultBuilder.error("Unable to parse URI for location: " + location);
        }
    }
}
