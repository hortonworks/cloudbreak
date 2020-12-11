package com.sequenceiq.cloudbreak.validation;

import static javax.validation.Validation.buildDefaultValidatorFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.validation.ValidCloudStorage;
import com.sequenceiq.common.model.CloudIdentityType;

class StorageIdentityValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testCloudStorageValidationWithoutStorageParameter() {
        StorageIdentityBase storage = new StorageIdentityBase();
        storage.setType(CloudIdentityType.LOG);

        Set<ConstraintViolation<StorageIdentityBase>> constraintViolations = validator.validate(storage);
        assertEquals(1, constraintViolations.size());

        ConstraintViolation<StorageIdentityBase> violation = constraintViolations.iterator().next();

        assertEquals(ValidCloudStorage.MESSAGE, violation.getMessage());
    }

    @Test
    void testCloudStorageValidationWithMoreThanOneStorageParameter() {
        StorageIdentityBase storage = new StorageIdentityBase();
        storage.setType(CloudIdentityType.LOG);
        S3CloudStorageV1Parameters s3 = new S3CloudStorageV1Parameters();
        s3.setInstanceProfile("instace::profile");
        storage.setS3(s3);
        GcsCloudStorageV1Parameters gcs = new GcsCloudStorageV1Parameters();
        gcs.setServiceAccountEmail("service.account@googlecloud.com");
        storage.setGcs(gcs);

        Set<ConstraintViolation<StorageIdentityBase>> constraintViolations = validator.validate(storage);
        assertEquals(1, constraintViolations.size());

        ConstraintViolation<StorageIdentityBase> violation = constraintViolations.iterator().next();

        assertEquals(ValidCloudStorage.MESSAGE, violation.getMessage());
    }

    @Test
    void testCloudStorageValidation() {
        StorageIdentityBase storage = new StorageIdentityBase();
        S3CloudStorageV1Parameters s3 = new S3CloudStorageV1Parameters();
        s3.setInstanceProfile("instace::profile");
        storage.setS3(s3);
        storage.setType(CloudIdentityType.LOG);

        Set<ConstraintViolation<StorageIdentityBase>> constraintViolations = validator.validate(storage);
        assertTrue(constraintViolations.isEmpty());
    }
}