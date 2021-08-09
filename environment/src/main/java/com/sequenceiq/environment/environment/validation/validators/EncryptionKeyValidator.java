package com.sequenceiq.environment.environment.validation.validators;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Component
public class EncryptionKeyValidator {

    private static final Pattern ENCRYPTION_KEY_PATTERN = Pattern.compile("projects\\/[a-zA-Z0-9_-]{1,63}\\/" +
            "locations\\/[a-zA-Z0-9_-]{1,63}\\/keyRings\\/[a-zA-Z0-9_-]{1,63}\\/cryptoKeys\\/[a-zA-Z0-9_-]{1,63}");

    public ValidationResult validateEncryptionKey(String encryptionKey) {
        ValidationResult.ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        Matcher matcher = ENCRYPTION_KEY_PATTERN.matcher(encryptionKey.trim());
        if (!matcher.matches()) {
            validationResultBuilder.error(String.format("Expected Format: '/projects/<projectName>/locations/<location>/" +
                    "keyRings/<keyRing>/cryptoKeys/<keyName>'. " +
                    "Key location should be same as resource location " +
                    "<keyName> may only contain alphanumeric characters and dashes."));
        }
        return validationResultBuilder.build();
    }
}