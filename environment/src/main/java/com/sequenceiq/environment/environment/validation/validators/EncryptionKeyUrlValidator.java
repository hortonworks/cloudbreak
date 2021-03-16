package com.sequenceiq.environment.environment.validation.validators;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Component
public class EncryptionKeyUrlValidator {

    private static final Pattern ENCRYPTION_KEY_URL_PATTERN = Pattern.compile("^https:\\/\\/[a-zA-Z-][0-9a-zA-Z-]*"
            + "\\.vault\\.azure\\.net\\/keys\\/[0-9a-zA-Z-]+\\/[0-9A-Za-z]+");

    public ValidationResult validateEncryptionKeyUrl(String encryptionKeyUrl) {
        ValidationResult.ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        Matcher matcher = ENCRYPTION_KEY_URL_PATTERN.matcher(encryptionKeyUrl.trim());
        if (!matcher.matches()) {
            validationResultBuilder.error(String.format("The encryption-key-url format is Invalid."
                + "Correct format is 'https://<vaultName>.vault.azure.net/keys/<keyName>/<keyVersion>' "
                + "where keyName can only contain alphanumeric characters & dashes and keyVersion can only contain alphanumeric characters."));
        }
        return validationResultBuilder.build();
    }
}