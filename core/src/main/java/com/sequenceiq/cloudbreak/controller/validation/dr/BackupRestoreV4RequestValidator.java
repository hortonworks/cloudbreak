package com.sequenceiq.cloudbreak.controller.validation.dr;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class BackupRestoreV4RequestValidator {

    private static final Pattern AWS_PATTERN = Pattern.compile("(^s3[a|n]?$)|(^$)", Pattern.CASE_INSENSITIVE);

    private static final Pattern AZURE_PATTERN = Pattern.compile("(^abfs[s]?$)|(^$)", Pattern.CASE_INSENSITIVE);

    public ValidationResult validate(Stack stack, String location, String backupId) {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        String cloudPlatform = stack.cloudPlatform();

        if (Strings.isNullOrEmpty(backupId)) {
            resultBuilder.error("Parameter backupId required");
        }
        if (Strings.isNullOrEmpty(location)) {
            resultBuilder.error("Parameter backupLocation required");
        } else {
            validateCloudLocationScheme(cloudPlatform, location, resultBuilder);
        }
        return resultBuilder.build();
    }

    private void validateCloudLocationScheme(String cloudPlatform, String location, ValidationResult.ValidationResultBuilder resultBuilder) {
        try {
            URI uri = new URI(location);
            if (AWS.equalsIgnoreCase(cloudPlatform)) {
                if (uri.getScheme() != null) {
                    Matcher matcher = AWS_PATTERN.matcher(uri.getScheme());
                    if (!matcher.find()) {
                        resultBuilder.error("Incorrect URL scheme for AWS cloud platform: " + location);
                    }
                }
            } else if (AZURE.equalsIgnoreCase(cloudPlatform)) {
                if (uri.getScheme() != null) {
                    Matcher matcher = AZURE_PATTERN.matcher(uri.getScheme());
                    if (!matcher.find()) {
                        resultBuilder.error("Incorrect URL scheme for Azure cloud platform: " + location);
                    }
                }
            } else {
                resultBuilder.error("Cloud platform " + cloudPlatform + " not supported for backup/restore");
            }
        } catch (URISyntaxException e) {
            resultBuilder.error("Unable to parse URI for location: " + location);
        }
    }
}
