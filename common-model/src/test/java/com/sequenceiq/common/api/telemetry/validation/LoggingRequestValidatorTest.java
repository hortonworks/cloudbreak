package com.sequenceiq.common.api.telemetry.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;

public class LoggingRequestValidatorTest {

    @InjectMocks
    private LoggingRequestValidator underTest;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    @BeforeEach
    public void setUp() {
        underTest = new LoggingRequestValidator();
        initMocks(this);
        when(context.buildConstraintViolationWithTemplate(any(String.class)).addConstraintViolation()).thenReturn(context);
    }

    @Test
    public void testValidateIfBothFieldsAreEmpty() {
        // GIVEN
        LoggingRequest loggingRequest = new LoggingRequest();
        // WHEN
        boolean result = underTest.isValid(loggingRequest, context);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testValidateInvalidLocation() {
        // GIVEN
        LoggingRequest loggingRequest = new LoggingRequest();
        loggingRequest.setS3(new S3CloudStorageV1Parameters());
        loggingRequest.setStorageLocation("?ds: da");
        // WHEN
        boolean result = underTest.isValid(loggingRequest, context);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testValidateValidBucket() {
        // GIVEN
        LoggingRequest loggingRequest = new LoggingRequest();
        loggingRequest.setS3(new S3CloudStorageV1Parameters());
        loggingRequest.setStorageLocation("mybucket");
        // WHEN
        boolean result = underTest.isValid(loggingRequest, context);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testValidateValidBucketUri() {
        // GIVEN
        LoggingRequest loggingRequest = new LoggingRequest();
        loggingRequest.setS3(new S3CloudStorageV1Parameters());
        loggingRequest.setStorageLocation("s3://mybucket/custom");
        // WHEN
        boolean result = underTest.isValid(loggingRequest, context);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testValidateValidBasePath() {
        // GIVEN
        LoggingRequest loggingRequest = new LoggingRequest();
        loggingRequest.setS3(new S3CloudStorageV1Parameters());
        loggingRequest.setStorageLocation("basePath/custom");
        // WHEN
        boolean result = underTest.isValid(loggingRequest, context);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testValidateValidBasePathUri() {
        // GIVEN
        LoggingRequest loggingRequest = new LoggingRequest();
        loggingRequest.setS3(new S3CloudStorageV1Parameters());
        loggingRequest.setStorageLocation("s3://basePath/custom");
        // WHEN
        boolean result = underTest.isValid(loggingRequest, context);
        // THEN
        assertTrue(result);
    }
}