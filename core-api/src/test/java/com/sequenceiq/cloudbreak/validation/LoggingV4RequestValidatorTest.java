package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.validation.ConstraintValidatorContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.telemetry.logging.LoggingV4Request;
import com.sequenceiq.cloudbreak.cloud.model.LoggingAttributesHolder;
import com.sequenceiq.cloudbreak.cloud.model.logging.S3LoggingAttributes;

public class LoggingV4RequestValidatorTest {

    @InjectMocks
    private LoggingV4RequestValidator underTest;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    @Before
    public void setUp() {
        underTest = new LoggingV4RequestValidator();
        initMocks(this);
        when(context.buildConstraintViolationWithTemplate(any(String.class)).addConstraintViolation()).thenReturn(context);
    }

    @Test
    public void testValidateIfBothFieldsAreEmpty() {
        // GIVEN
        LoggingV4Request loggingV4Request = new LoggingV4Request();
        LoggingAttributesHolder attributesHolder = new LoggingAttributesHolder();
        S3LoggingAttributes s3Attributes = new S3LoggingAttributes(null, null, null);
        attributesHolder.setS3Attributes(s3Attributes);
        loggingV4Request.setAttributes(attributesHolder);
        // WHEN
        boolean result = underTest.isValid(loggingV4Request, context);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testValidateInvalidBucket() {
        // GIVEN
        LoggingV4Request loggingV4Request = new LoggingV4Request();
        LoggingAttributesHolder attributesHolder = new LoggingAttributesHolder();
        S3LoggingAttributes s3Attributes = new S3LoggingAttributes("?ds: da", null, null);
        attributesHolder.setS3Attributes(s3Attributes);
        loggingV4Request.setAttributes(attributesHolder);
        // WHEN
        boolean result = underTest.isValid(loggingV4Request, context);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testValidateInvalidBasePath() {
        // GIVEN
        LoggingV4Request loggingV4Request = new LoggingV4Request();
        LoggingAttributesHolder attributesHolder = new LoggingAttributesHolder();
        S3LoggingAttributes s3Attributes = new S3LoggingAttributes(null, "?ds: da", null);
        attributesHolder.setS3Attributes(s3Attributes);
        loggingV4Request.setAttributes(attributesHolder);
        // WHEN
        boolean result = underTest.isValid(loggingV4Request, context);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testValidateValidBucket() {
        // GIVEN
        LoggingV4Request loggingV4Request = new LoggingV4Request();
        LoggingAttributesHolder attributesHolder = new LoggingAttributesHolder();
        S3LoggingAttributes s3Attributes = new S3LoggingAttributes("mybucket", null, null);
        attributesHolder.setS3Attributes(s3Attributes);
        loggingV4Request.setAttributes(attributesHolder);
        // WHEN
        boolean result = underTest.isValid(loggingV4Request, context);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testValidateValidBucketUri() {
        // GIVEN
        LoggingV4Request loggingV4Request = new LoggingV4Request();
        LoggingAttributesHolder attributesHolder = new LoggingAttributesHolder();
        S3LoggingAttributes s3Attributes = new S3LoggingAttributes("s3://mybucket/custom", null, null);
        attributesHolder.setS3Attributes(s3Attributes);
        loggingV4Request.setAttributes(attributesHolder);
        // WHEN
        boolean result = underTest.isValid(loggingV4Request, context);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testValidateValidBasePath() {
        // GIVEN
        LoggingV4Request loggingV4Request = new LoggingV4Request();
        LoggingAttributesHolder attributesHolder = new LoggingAttributesHolder();
        S3LoggingAttributes s3Attributes = new S3LoggingAttributes(null, "basePath/custom", null);
        attributesHolder.setS3Attributes(s3Attributes);
        loggingV4Request.setAttributes(attributesHolder);
        // WHEN
        boolean result = underTest.isValid(loggingV4Request, context);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testValidateValidBasePathUri() {
        // GIVEN
        LoggingV4Request loggingV4Request = new LoggingV4Request();
        LoggingAttributesHolder attributesHolder = new LoggingAttributesHolder();
        S3LoggingAttributes s3Attributes = new S3LoggingAttributes(null, "s3://basePath/custom", null);
        attributesHolder.setS3Attributes(s3Attributes);
        loggingV4Request.setAttributes(attributesHolder);
        // WHEN
        boolean result = underTest.isValid(loggingV4Request, context);
        // THEN
        assertTrue(result);
    }
}
