package com.sequenceiq.common.api.telemetry.validation;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.common.api.telemetry.request.LoggingRequest;

public class LoggingRequestValidator implements ConstraintValidator<ValidLoggingRequest, LoggingRequest> {

    @Override
    public void initialize(ValidLoggingRequest constraintAnnotation) {
    }

    @Override
    public boolean isValid(LoggingRequest value, ConstraintValidatorContext context) {
        if (value != null) {
            if (StringUtils.isEmpty(value.getStorageLocation())) {
                String msg = "Storage location paramater is empty in logging request";
                context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
                return false;
            }
            if (value.getS3() != null && !isValidPath(value.getStorageLocation())) {
                String msg = "Storage location paramater is invalid (s3) in logging request";
                context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
                return false;
            }
            if (value.getS3() == null && value.getAdlsGen2() == null) {
                String msg = "Provide at least 1 cloud storage details in logging request";
                context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
                return false;
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
