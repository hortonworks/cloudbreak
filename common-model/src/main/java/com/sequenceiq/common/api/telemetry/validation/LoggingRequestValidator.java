package com.sequenceiq.common.api.telemetry.validation;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.model.FileSystemType;

public class LoggingRequestValidator implements ConstraintValidator<ValidLoggingRequest, LoggingRequest> {

    @Override
    public void initialize(ValidLoggingRequest constraintAnnotation) {
    }

    @Override
    public boolean isValid(LoggingRequest value, ConstraintValidatorContext context) {
        if (value != null) {
            if (StringUtils.isEmpty(value.getStorageLocation())) {
                String msg = "Storage location parameter is empty in logging request";
                context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
                return false;
            }
            if (isProviderSpecificDataProvided(value) && !isValidPath(value)) {
                String msg = "Storage location path is invalid in logging request";
                context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
                return false;
            }
            if (value.getS3() == null && value.getAdlsGen2() == null && value.getGcs() == null) {
                String msg = "Provide at least 1 cloud storage detail in logging request";
                context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
                return false;
            }
        }
        return true;
    }

    public boolean isProviderSpecificDataProvided(LoggingRequest value) {
        return value.getS3() != null || value.getAdlsGen2() != null || value.getGcs() != null;
    }

    private boolean isValidPath(LoggingRequest value) {
        boolean valid = false;
        try {
            Paths.get(value.getStorageLocation());
            if (value.getS3() != null && isValidPathPrefix(value, FileSystemType.S3)) {
                valid = true;
            } else if (value.getGcs() != null && isValidPathPrefix(value, FileSystemType.GCS)) {
                valid = true;
            } else if (value.getAdlsGen2() != null && isValidPathPrefix(value, FileSystemType.ADLS_GEN_2)) {
                valid = true;
            }
        } catch (InvalidPathException ex) {
            // valid = false
        }
        return valid && !StringUtils.containsWhitespace(value.getStorageLocation());
    }

    private boolean isValidPathPrefix(LoggingRequest value, FileSystemType fileSystemType) {
        boolean valid = false;
        for (String loggingProtocol : fileSystemType.getLoggingProtocol()) {
            if (value.getStorageLocation().startsWith(loggingProtocol + "://")) {
                valid = true;
            }
        }
        return valid;
    }
}
