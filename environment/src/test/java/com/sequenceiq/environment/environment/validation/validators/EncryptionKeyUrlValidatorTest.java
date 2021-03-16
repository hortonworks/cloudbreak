package com.sequenceiq.environment.environment.validation.validators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.validation.ValidationResult;

class EncryptionKeyUrlValidatorTest {

    private EncryptionKeyUrlValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new EncryptionKeyUrlValidator();
    }

    @Test
    void testEncryptionKeyUrlValidatonWithValidKeyAndCommentIsValid() {
        String validKey = "https://someVault.vault.azure.net/keys/someKey/someKeyVersion";

        ValidationResult validationResult = underTest.validateEncryptionKeyUrl(validKey);
        assertFalse(validationResult.hasError());
    }

    @Test
    void testEncryptionKeyUrlValidatonWithInValidKeyAndCommentIsValid() {
        String invalidKey = "https://wrong_format.vault.azure.net/keys/wrong_format/wrong_format";

        ValidationResult validationResult = underTest.validateEncryptionKeyUrl(invalidKey);
        assertTrue(validationResult.hasError());
    }
}