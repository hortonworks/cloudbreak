package com.sequenceiq.cloudbreak.validation;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.telemetry.logging.LoggingV4Request;
import com.sequenceiq.cloudbreak.cloud.model.logging.S3LoggingAttributes;

public class LoggingV4RequestValidator implements ConstraintValidator<ValidLoggingV4Request, LoggingV4Request> {

    private static final String INVALID_PATH_MSG = "Field '%s' has invalid format: %s";

    @Override
    public void initialize(ValidLoggingV4Request constraintAnnotation) {
    }

    @Override
    public boolean isValid(LoggingV4Request value, ConstraintValidatorContext context) {
        if (value != null && value.getAttributes() != null) {
            S3LoggingAttributes s3Attributes = value.getAttributes().getS3Attributes();
            if (s3Attributes != null) {
                String bucket = s3Attributes.getBucket();
                String basePath = s3Attributes.getBasePath();
                if (StringUtils.isAllEmpty(bucket, basePath)) {
                    String msg = "Field 'bucket' and/or 'basePath' needs to be provided for s3 logging attributes";
                    context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
                    return false;
                }
                if (StringUtils.isNotEmpty(bucket) && !isValidPath(bucket)) {
                    String msg = String.format(INVALID_PATH_MSG, "bucket", bucket);
                    context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
                    return false;
                }

                if (StringUtils.isNotEmpty(basePath) && !isValidPath(basePath)) {
                    String msg = String.format(INVALID_PATH_MSG, "basePath", basePath);
                    context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValidPath(String path) {
        try {
            Paths.get(path);
        } catch (InvalidPathException ex) {
            return false;
        }
        return !StringUtils.containsWhitespace(path);
    }
}
