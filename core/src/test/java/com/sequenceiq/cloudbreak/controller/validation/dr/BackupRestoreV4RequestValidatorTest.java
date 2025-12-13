package com.sequenceiq.cloudbreak.controller.validation.dr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@ExtendWith(MockitoExtension.class)
class BackupRestoreV4RequestValidatorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupRestoreV4RequestValidatorTest.class);

    private static final String BACKUP_ID = "backupId";

    private static final String LOCATION = "test/backup";

    private static final String INCORRECT_SCHEME_ERROR_MSG = "Incorrect scheme \"%s\" for cloud platform \"%s\"";

    private static final String MISSING_BACKUP_ID_ERROR_MSG = "Parameter backupId required";

    private static final String MISSING_BACKUP_LOCATION_ERROR_MSG = "Parameter backupLocation required";

    private static final String UNSUPPORTED_CLOUD_PROVIDER_ERROR_MSG = "Cloud platform \"%s\" not supported for backup/restore";

    private static final String UNSUPPORTED_CLOUD_PROVIDER_SCHEME_ERROR_MSG = "Unsupported cloud platform, can't validate scheme";

    private static final List<String> BASE_VALID_LOCATION_PREFIXES = Arrays.asList("", "/", "hdfs://");

    private static final List<String> AWS_VALID_SCHEMES_CASE_INSENSITIVE = Arrays.asList("s3", "s3a", "s3n");

    private static final List<String> AZURE_VALID_SCHEMES_CASE_INSENSITIVE = Arrays.asList("abfs", "abfss");

    private static final List<String> GCP_VALID_SCHEMES_CASE_INSENSITIVE = Collections.singletonList("gs");

    @InjectMocks
    private BackupRestoreV4RequestValidator requestValidator;

    @Test
    void testAWSSuccessfulValidation() {
        testValidSchemes(CloudPlatform.AWS.name(), AWS_VALID_SCHEMES_CASE_INSENSITIVE);
    }

    @Test
    void testAWSWrongScheme() {
        Stack stack = getStack(CloudPlatform.AWS.name());
        String backupLocation = "abfs://" + LOCATION;
        ValidationResult validationResult = requestValidator.validate(stack, backupLocation, BACKUP_ID);
        assert validationResult.hasError();
        assertEquals(String.format(INCORRECT_SCHEME_ERROR_MSG, "abfs", "AWS"), validationResult.getFormattedErrors());
    }

    @Test
    void testAzureSuccessfulValidation() {
        testValidSchemes(CloudPlatform.AZURE.name(), AZURE_VALID_SCHEMES_CASE_INSENSITIVE);
    }

    @Test
    void testAzureWrongScheme() {
        Stack stack = getStack(CloudPlatform.AZURE.name());
        String backupLocation = "s3://test/backup";
        ValidationResult validationResult = requestValidator.validate(stack, backupLocation, BACKUP_ID);
        assert validationResult.hasError();
        assertEquals(String.format(INCORRECT_SCHEME_ERROR_MSG, "s3", "AZURE"), validationResult.getFormattedErrors());
    }

    @Test
    void testGCPSuccessfulValidation() {
        testValidSchemes(CloudPlatform.GCP.name(), GCP_VALID_SCHEMES_CASE_INSENSITIVE);
    }

    @Test
    void testGCPWrongScheme() {
        Stack stack = getStack(CloudPlatform.GCP.name());
        String backupLocation = "s3a://test/backup";
        ValidationResult validationResult = requestValidator.validate(stack, backupLocation, BACKUP_ID);
        assert validationResult.hasError();
        assertEquals(String.format(INCORRECT_SCHEME_ERROR_MSG, "s3a", "GCP"), validationResult.getFormattedErrors());
    }

    @Test
    void testUnsupportedCloudPlatformWithoutScheme() {
        Stack stack = getStack(CloudPlatform.YARN.name());
        ValidationResult validationResult = requestValidator.validate(stack, LOCATION, BACKUP_ID);
        assert validationResult.hasError();
        assertEquals(String.format(UNSUPPORTED_CLOUD_PROVIDER_ERROR_MSG, "YARN"), validationResult.getFormattedErrors());
    }

    @Test
    void testUnsupportedCloudPlatformWithScheme() {
        Stack stack = getStack(CloudPlatform.YARN.name());
        ValidationResult validationResult = requestValidator.validate(stack, "s3a://" + LOCATION, BACKUP_ID);
        assert validationResult.hasError();
        String expectedErrorMessage = String.format("1. %s\n2. %s",
                String.format(UNSUPPORTED_CLOUD_PROVIDER_ERROR_MSG, "YARN"),
                UNSUPPORTED_CLOUD_PROVIDER_SCHEME_ERROR_MSG);
        assertEquals(expectedErrorMessage, validationResult.getFormattedErrors());
    }

    @Test
    void testMissingBackupId() {
        Stack stack = getStack(CloudPlatform.AWS.name());
        ValidationResult validationResult = requestValidator.validate(stack, LOCATION, null);
        assert validationResult.hasError();
        assertEquals(MISSING_BACKUP_ID_ERROR_MSG, validationResult.getFormattedErrors());
    }

    @Test
    void testMissingBackupLocation() {
        Stack stack = getStack(CloudPlatform.AWS.name());
        ValidationResult validationResult = requestValidator.validate(stack, null, BACKUP_ID);
        assert validationResult.hasError();
        assertEquals(MISSING_BACKUP_LOCATION_ERROR_MSG, validationResult.getFormattedErrors());
    }

    @Test
    void testMultipleErrorsUnsupportedPlatformNullLocationNullBackupId() {
        Stack stack = getStack(CloudPlatform.YARN.name());
        ValidationResult validationResult = requestValidator.validate(stack, null, null);
        assert validationResult.hasError();
        String expectedErrorMessage = String.format("1. %s\n2. %s\n3. %s",
                String.format(UNSUPPORTED_CLOUD_PROVIDER_ERROR_MSG, "YARN"),
                MISSING_BACKUP_ID_ERROR_MSG,
                MISSING_BACKUP_LOCATION_ERROR_MSG);
        assertEquals(expectedErrorMessage, validationResult.getFormattedErrors());
    }

    @Test
    void testMultipleErrorsUnsupportedPlatformValidLocationNullBackupId() {
        Stack stack = getStack(CloudPlatform.YARN.name());
        String backupLocation = "gs://test/backup";
        ValidationResult validationResult = requestValidator.validate(stack, backupLocation, null);
        assert validationResult.hasError();
        String expectedErrorMessage = String.format("1. %s\n2. %s\n3. %s",
                String.format(UNSUPPORTED_CLOUD_PROVIDER_ERROR_MSG, "YARN"),
                MISSING_BACKUP_ID_ERROR_MSG,
                UNSUPPORTED_CLOUD_PROVIDER_SCHEME_ERROR_MSG);
        assertEquals(expectedErrorMessage, validationResult.getFormattedErrors());
    }

    @Test
    void testMultipleErrorsSupportedPlatformInvalidLocationNullBackupId() {
        Stack stack = getStack(CloudPlatform.GCP.name());
        String backupLocation = "s3a://test/backup";
        ValidationResult validationResult = requestValidator.validate(stack, backupLocation, null);
        assert validationResult.hasError();
        String expectedErrorMessage = String.format("1. %s\n2. %s",
                String.format(INCORRECT_SCHEME_ERROR_MSG, "s3a", "GCP"),
                MISSING_BACKUP_ID_ERROR_MSG);
        assertEquals(expectedErrorMessage, validationResult.getFormattedErrors());
    }

    private void testValidSchemes(String cloudPlatform, List<String> cloudPlatformValidSchemesCaseInsensitive) {
        Stack stack = getStack(cloudPlatform);

        // As we are using regex, we assume fully capitalized versions of the schemas suffice for testing.
        List<String> cloudPlatformValidPrefixes = cloudPlatformValidSchemesCaseInsensitive.stream()
                .flatMap(scheme -> Stream.of(scheme + "://", scheme.toUpperCase(Locale.ROOT) + "://")).collect(Collectors.toList());

        Iterable<String> validPrefixes = Iterables.concat(BASE_VALID_LOCATION_PREFIXES, cloudPlatformValidPrefixes);
        boolean validationPassed = true;
        for (String prefix : validPrefixes) {
            ValidationResult validationResult = requestValidator.validate(stack, prefix + LOCATION, BACKUP_ID);
            if (validationResult.hasError()) {
                LOGGER.error(validationResult.getFormattedErrors());
                validationPassed = false;
            }
        }

        assertTrue(validationPassed, "Validation failed for valid schemes under cloud platform \"" + cloudPlatform +
                "\"! Check logs for the exact schemes that failed.");
    }

    private Stack getStack(String cloudPlatform) {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setEnvironmentCrn("env-crn");
        stack.setCloudPlatform(cloudPlatform);
        stack.setPlatformVariant(cloudPlatform);
        stack.setRegion("eu-central-1");
        Blueprint blueprint = new Blueprint();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);
        return stack;
    }
}
