package com.sequenceiq.environment.api.v1.environment.model.request.aws;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwsDiskEncryptionParametersTest {

    private static final String ENCRYPTION_KEY_ARN = "encryptionKeyArn";

    @Test
    void defaultConstructorTest() {
        AwsDiskEncryptionParameters underTest = new AwsDiskEncryptionParameters();
        underTest.setEncryptionKeyArn(ENCRYPTION_KEY_ARN);
        verifyFields(underTest);
    }

    private void verifyFields(AwsDiskEncryptionParameters underTest) {
        assertThat(underTest.getEncryptionKeyArn()).isEqualTo(ENCRYPTION_KEY_ARN);
    }

    @Test
    void builderTest() {
        AwsDiskEncryptionParameters underTest = AwsDiskEncryptionParameters.builder()
                .withEncryptionKeyArn(ENCRYPTION_KEY_ARN)
                .build();
        verifyFields(underTest);
    }

    static Object[][] encryptionKeyArnValidationTestDataProvider() {
        return new Object[][]{
                // testCaseName encryptionKeyArn expectedValid
                {"encryptionKeyArnWithoutKeyID/", "arn:aws:kms:us-east-1:012345678910:key/", false},

                {"EncryptionKeyArnWithoutPartition",
                        "aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab", false},

                {"encryptionKeyArnWithNonNumericAccountId",
                        "arn:aws:kms:us-east-1:abc:key/1234abcd-12ab-34cd-56ef-1234567890ab", false},

                {"encryptionKeyArnWithInvalidServiceName",
                        "arn:aws:role:us-east-1:012345678910/1234abcd-12ab-34cd-56ef-1234567890ab", false},

                {"encryptionKeyArnWithInvalidRegion",
                        "arn:aws-us-gov:kms:us-east/2:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab", false},

                {"encryptionKeyArnWithSpecialCharInAccountId",
                        "arn:aws-us-gov:kms:us-east-2:0123456789-:key/1234abcd-12ab-34cd-56ef-1234567890ab", false},

                {"encryptionKeyArnWithoutRegion",
                        "arn:aws-us-gov:kms::0123456789:key/1234abcd-12ab-34cd-56ef-1234567890ab", false},

                {"encryptionKeyArnWithoutAccountId",
                        "arn:aws-us-gov:kms:us-east-1::key/1234abcd-12ab-34cd-56ef-1234567890ab", false},

                {"EncryptionKeyArnWithInvalidKeyIDSubStringCount",
                        "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd", false},

                {"EncryptionKeyArnWithInvalidKeyIDSubStringLength",
                        "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56-1234567890ab", false},

                {"encryptionKeyArnWithValidKey",
                        "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab", true},

                {"encryptionKeyArnWithPossibleValidPartition",
                        "arn:aws-us-gov:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab", true},

                {"encryptionKeyArnWithPossibleValidPartition",
                        "arn:aws-cn:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab", true},

                {"encryptionKeyArnWithPossibleValidRegion",
                        "arn:aws-us-gov:kms:us-east-2:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab", true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("encryptionKeyArnValidationTestDataProvider")
    void encryptionKeyArnValidationTest(String testCaseName, String encryptionKeyArn, boolean expectedValid) {
        if (expectedValid) {
            assertTrue(encryptionKeyArn.matches(AwsDiskEncryptionParameters.REG_EXP_FOR_ENCRYPTION_KEY_ARN),
                    encryptionKeyArn + " should match pattern:" + AwsDiskEncryptionParameters.AWS_ENCRYPTION_KEY_INVALID_MESSAGE);
        } else {
            assertFalse(encryptionKeyArn.matches(AwsDiskEncryptionParameters.REG_EXP_FOR_ENCRYPTION_KEY_ARN),
                    encryptionKeyArn + " should match pattern: " + AwsDiskEncryptionParameters.AWS_ENCRYPTION_KEY_INVALID_MESSAGE);
        }
    }
}