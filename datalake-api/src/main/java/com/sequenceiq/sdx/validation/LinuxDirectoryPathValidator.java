package com.sequenceiq.sdx.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LinuxDirectoryPathValidator implements ConstraintValidator<ValidLinuxDirectoryPath, String> {

    // Disallows null char (\x00) and '/' inside segments
    private static final String LINUX_PATH_REGEX = "^/([^/\\\\\\u0000]+(/)?)*$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return trimmed.matches(LINUX_PATH_REGEX);
    }
}