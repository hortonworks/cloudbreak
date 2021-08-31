package com.sequenceiq.environment.environment.validation.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.validation.ValidationResult;

class EncryptionKeyArnValidatorTest {

    private EncryptionKeyArnValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new EncryptionKeyArnValidator();
    }

    @Test
    void testEncryptionKeyArnValidationWithValidKeyAndCommentIsValid() {
        String validKey = "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab";

        ValidationResult validationResult = underTest.validateEncryptionKeyArn(validKey);
        assertFalse(validationResult.hasError());

    }

    @Test
    void testEncryptionKeyUrlValidationWithInValidKeyAndCommentIsValid() {
        String invalidKey = "aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab";

        ValidationResult validationResult = underTest.validateEncryptionKeyArn(invalidKey);
        assertTrue(validationResult.hasError());
        assertEquals(String.format("The identifier of the AWS Key Management Service (AWS KMS)" +
                        "customer master key (CMK) to use for Amazon EBS encryption.%n" +
                        "You can specify the key ARN in the below format:%n" +
                        "Key ARN: arn:partition:service:region:account-id:resource-type/resource-id. " +
                        "For example, arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab.%n"),
                validationResult.getFormattedErrors());
    }
}