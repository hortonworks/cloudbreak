package com.sequenceiq.cloudbreak.controller.validation.dr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BackupRestoreV4RequestValidatorTest {

    private static final String BACKUP_ID = "backupId";

    private static final String LOCATION = "test/backup";

    @InjectMocks
    private BackupRestoreV4RequestValidator requestValidator;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAWSSuccessfulValidation() {
        Stack stack = getStack(CloudPlatform.AWS.name());

        ValidationResult validationResult = requestValidator.validate(stack, "s3://" + LOCATION, BACKUP_ID);
        assertFalse(validationResult.hasError());

        validationResult = requestValidator.validate(stack, "s3a://" + LOCATION, BACKUP_ID);
        assertFalse(validationResult.hasError());

        validationResult = requestValidator.validate(stack, "s3n://" + LOCATION, BACKUP_ID);
        assertFalse(validationResult.hasError());

        validationResult = requestValidator.validate(stack, "S3://" + LOCATION, BACKUP_ID);
        assertFalse(validationResult.hasError());

        validationResult = requestValidator.validate(stack, "S3a://" + LOCATION, BACKUP_ID);
        assertFalse(validationResult.hasError());

        validationResult = requestValidator.validate(stack, "S3n://" + LOCATION, BACKUP_ID);
        assertFalse(validationResult.hasError());

        validationResult = requestValidator.validate(stack, '/' + LOCATION, BACKUP_ID);
        assertFalse(validationResult.hasError());

        validationResult = requestValidator.validate(stack, LOCATION, BACKUP_ID);
        assertFalse(validationResult.hasError());

        validationResult = requestValidator.validate(stack, "hdfs://" + LOCATION, BACKUP_ID);
        assertFalse(validationResult.hasError());
    }

    @Test
    public void testAzureSuccessfulValidation() {
        Stack stack = getStack(CloudPlatform.AZURE.name());

        ValidationResult validationResult = requestValidator.validate(stack, "abfs://" + LOCATION, BACKUP_ID);
        assertFalse(validationResult.hasError());

        validationResult = requestValidator.validate(stack, "abfss://" + LOCATION, BACKUP_ID);
        assertFalse(validationResult.hasError());

        validationResult = requestValidator.validate(stack, "ABFS://" + LOCATION, BACKUP_ID);
        assertFalse(validationResult.hasError());

        validationResult = requestValidator.validate(stack, "ABFSS://" + LOCATION, BACKUP_ID);
        assertFalse(validationResult.hasError());
        assertFalse(validationResult.hasError());

        validationResult = requestValidator.validate(stack, '/' + LOCATION, BACKUP_ID);
        assertFalse(validationResult.hasError());

        validationResult = requestValidator.validate(stack, LOCATION, BACKUP_ID);
        assertFalse(validationResult.hasError());

        validationResult = requestValidator.validate(stack, "hdfs://" + LOCATION, BACKUP_ID);
        assertFalse(validationResult.hasError());
    }

    @Test
    public void testAWSWrongScheme() {
        Stack stack = getStack(CloudPlatform.AWS.name());
        String backupLocation = "abfs://" + LOCATION;
        ValidationResult validationResult = requestValidator.validate(stack, backupLocation, BACKUP_ID);
        assert validationResult.hasError();
        assertEquals("Incorrect URL scheme for AWS cloud platform: " + backupLocation, validationResult.getFormattedErrors());
    }

    @Test
    public void testAzureWrongScheme() {
        Stack stack = getStack(CloudPlatform.AZURE.name());
        String backupLocation = "s3://test/backup";
        ValidationResult validationResult = requestValidator.validate(stack, backupLocation, BACKUP_ID);
        assert validationResult.hasError();
        assertEquals("Incorrect URL scheme for Azure cloud platform: " + backupLocation, validationResult.getFormattedErrors());
    }

    @Test
    public void testUnsupportedCloudPlatform() {
        Stack stack = getStack(CloudPlatform.YARN.name());
        ValidationResult validationResult = requestValidator.validate(stack, LOCATION, BACKUP_ID);
        assert validationResult.hasError();
        assertEquals("Cloud platform YARN not supported for backup/restore", validationResult.getFormattedErrors());
    }

    @Test
    public void testMissingBackupId() {
        Stack stack = getStack(CloudPlatform.AWS.name());
        ValidationResult validationResult = requestValidator.validate(stack, LOCATION, null);
        assert validationResult.hasError();
        assertEquals("Parameter backupId required", validationResult.getFormattedErrors());
    }

    @Test
    public void testMissingBackupLocation() {
        Stack stack = getStack(CloudPlatform.AWS.name());
        ValidationResult validationResult = requestValidator.validate(stack, null, BACKUP_ID);
        assert validationResult.hasError();
        assertEquals("Parameter backupLocation required", validationResult.getFormattedErrors());
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
