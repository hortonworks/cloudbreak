package com.sequenceiq.environment.api.v1.environment.model.request.aws;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import javax.annotation.RegEx;
import javax.validation.ConstraintViolation;
import javax.validation.constraints.Pattern;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hibernate.validator.HibernateValidator;
import org.intellij.lang.annotations.RegExp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

class AWSDiskEncryptionParametersTest {

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
                {"encryptionKeyArn=null", null, true},
                {"encryptionKeyArn=ENCRYPTION_KEY_ARN", ENCRYPTION_KEY_ARN, false},
                {"encryptionKeyArn=arn:aws:kms:us-east-1:012345678910:key/", "arn:aws:kms:us-east-1:012345678910:key/", false},

                {"encryptionKeyArn=aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab",
                        "aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab", false},

                {"encryptionKeyArn=arn:aws:kms:us-east-1:abc:key/1234abcd-12ab-34cd-56ef-1234567890ab",
                        "arn:aws:kms:us-east-1:abc:key/1234abcd-12ab-34cd-56ef-1234567890ab", false},

                {"encryptionKeyArn=arn:aws:role:us-east-1:012345678910/1234abcd-12ab-34cd-56ef-1234567890ab",
                        "arn:aws:role:us-east-1:012345678910/1234abcd-12ab-34cd-56ef-1234567890ab", false},

                {"encryptionKeyArn=arn:aws-us-gov:kms:us-east/2:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab",
                        "arn:aws-us-gov:kms:us-east/2:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab", false},

                {"encryptionKeyArn=arn:aws-us-gov:kms:us-east-2:0123456789-:key/1234abcd-12ab-34cd-56ef-1234567890ab",
                        "arn:aws-us-gov:kms:us-east-2:0123456789-:key/1234abcd-12ab-34cd-56ef-1234567890ab", false},

                {"encryptionKeyArn=arn:aws-us-gov:kms::0123456789:key/1234abcd-12ab-34cd-56ef-1234567890ab",
                        "arn:aws-us-gov:kms::0123456789:key/1234abcd-12ab-34cd-56ef-1234567890ab", false},

                {"encryptionKeyArn=arn:aws-us-gov:kms:us-east-2::key/1234abcd-12ab-34cd-56ef-1234567890ab",
                        "arn:aws-us-gov:kms:us-east-1::key/1234abcd-12ab-34cd-56ef-1234567890ab", false},

                {"encryptionKeyArn=arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab",
                        "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab", true},

                {"encryptionKeyArn=arn:aws-us-gov:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab",
                        "arn:aws-us-gov:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab", true},

                {"encryptionKeyArn=arn:aws-cn:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab",
                        "arn:aws-cn:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab", true},

                {"encryptionKeyArn=arn:aws-us-gov:kms:us-east-2:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab",
                        "arn:aws-us-gov:kms:us-east-2:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab", true},

        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("encryptionKeyArnValidationTestDataProvider")
    void encryptionKeyArnValidationTest(String testCaseName, String encryptionKeyArn, boolean expectedValid) {
        AwsDiskEncryptionParameters underTest = AwsDiskEncryptionParameters.builder()
                .withEncryptionKeyArn(encryptionKeyArn)
                .build();
        ;

        Matcher match = Pattern.compile("^arn:(aws|aws-cn|aws-us-gov):kms:[a-zA-Z0-9-]+:[0-9]+:key/[a-zA-Z0-9-]+$");

        if (expectedValid) {
            assertTrue(match.matches());
            assertEquals(testCaseName, match);

        } else
            assertFalse(testCaseName, match);

        MatcherAssert.assertThat(testCaseName, match, expectedValid);

        Set<ConstraintViolation<AwsDiskEncryptionParameters>> constraintViolations = localValidatorFactory.validate(underTest);

        if (expectedValid) {
            assertThat(constraintViolations
                    .stream()
                    .noneMatch(cv -> AwsDiskEncryptionParameters.AWS_ENCRYPTION_KEY_INVALID_MESSAGE.equals(cv.getMessage()))
            ).isTrue();
        } else {
            assertThat(constraintViolations
                    .stream()
                    .anyMatch(cv -> AwsDiskEncryptionParameters.AWS_ENCRYPTION_KEY_INVALID_MESSAGE.equals(cv.getMessage()))
            ).isTrue();
        }
    }

}