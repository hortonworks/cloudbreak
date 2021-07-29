package com.sequenceiq.environment.environment.validation.validators;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Component
public class EncryptionKeyValidator {

    private static final Pattern ENCRYPTION_KEY_PATTERN = Pattern.compile(".*");

    public ValidationResult validateEncryptionKey(String encryptionKey) {
        ValidationResult.ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        Matcher matcher = ENCRYPTION_KEY_PATTERN.matcher(encryptionKey.trim());
        if (!matcher.matches()) {
            validationResultBuilder.error(String.format("Expected Format: '/projects/<projectName>/locations/<location>/" +
                    "keyRings/<KeyRing>/cryptoKeys/<Key name>/cryptoKeyVersions/<version>'. " +
                    "Key location should be same as resource location " +
                    "<keyName> may only contain alphanumeric characters and dashes."));
        }
        return validationResultBuilder.build();
    }
}