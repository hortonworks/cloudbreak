package com.sequenceiq.environment.environment.validation.validators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.validation.ValidationResult;

class EncryptionKeyValidatorTest {

    private EncryptionKeyValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new EncryptionKeyValidator();
    }

    @Test
    void testEncryptionKeyUrlValidationWithValidKeyAndCommentIsValid() {
        String validKey = "projects/dummy-project/locations/us-west2/keyRings/dummy-ring/cryptoKeys/dummy-key";

        ValidationResult validationResult = underTest.validateEncryptionKey(validKey);
        assertFalse(validationResult.hasError());
    }

    @Test
    void testEncryptionKeyUrlValidationWithInValidKeyAndCommentIsValid() {
        String invalidKey = "projects/Invalid-project/locations/us-west2/keyRings/Invalid-ring/cryptoKeys/Invalid-key\"";

        ValidationResult validationResult = underTest.validateEncryptionKey(invalidKey);
        assertTrue(validationResult.hasError());
    }
}