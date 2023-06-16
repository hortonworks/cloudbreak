package com.sequenceiq.environment.environment.validation.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;

class EnvironmentAuthenticationValidatorTest {

    private static final String MALFORMED_PUBLIC_KEY = "AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
            + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
            + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
            + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
            + "KR495VFmuOepLYz5I8Dn sequence-eu";

    private static final String VALID_PUBLIC_KEY_RSA = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
            + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
            + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
            + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
            + "KR495VFmuOepLYz5I8Dn sequence-eu";

    private static final String VALID_PUBLIC_KEY_ED25519 = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIMQltFutaGpkyuDLScqHRZtknBd4c/IJCkVsY7WFS+gK";

    private final EnvironmentAuthenticationValidator underTest = new EnvironmentAuthenticationValidator();

    @Test
    void testValidateShouldReturnValidationErrorWhenMalformedKey() {
        EnvironmentValidationDto environmentValidationDto = createEnvironmentDto(MALFORMED_PUBLIC_KEY, null);
        String expected = "Failed to parse public key. Detailed message: Corrupt or unknown public key file format";

        ValidationResult actual = underTest.validate(environmentValidationDto);

        assertEquals(expected, actual.getFormattedErrors());
    }

    @Test
    void testValidateShouldReturnWithoutErrorWhenRsa() {
        EnvironmentValidationDto environmentValidationDto = createEnvironmentDto(VALID_PUBLIC_KEY_RSA, null);

        ValidationResult actual = underTest.validate(environmentValidationDto);

        assertFalse(actual.hasError());
    }

    @Test
    void testValidateShouldReturnWithoutErrorWhenPublicKeyIdIsPresent() {
        EnvironmentValidationDto environmentValidationDto = createEnvironmentDto(null, "public-key-id");

        ValidationResult actual = underTest.validate(environmentValidationDto);

        assertFalse(actual.hasError());
    }

    @Test
    void testValidateShouldReturnValidationErrorWhenEd25519AndAwsGovCloud() {
        EnvironmentValidationDto environmentValidationDto = createEnvironmentDto("AWS", true, VALID_PUBLIC_KEY_ED25519, null);
        String expected = "Failed to parse public key. Detailed message: SSH2ED25519: this key type is not allowed when running clusters in FIPS mode";

        ValidationResult actual = underTest.validate(environmentValidationDto);

        assertEquals(expected, actual.getFormattedErrors());
    }

    static Object[][] ed25519AndNotAwsGovCloudDataProvider() {
        return new Object[][]{
                // cloudPlatform, govCloud
                {"AWS", null},
                {"AWS", false},
                {"AZURE", null},
                {"AZURE", false},
        };
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("ed25519AndNotAwsGovCloudDataProvider")
    void testValidateShouldReturnWithoutErrorWhenEd25519AndNotAwsGovCloud(String cloudPlatform, Boolean govCloud) {
        EnvironmentValidationDto environmentValidationDto = createEnvironmentDto(cloudPlatform, govCloud, VALID_PUBLIC_KEY_ED25519, null);

        ValidationResult actual = underTest.validate(environmentValidationDto);

        assertFalse(actual.hasError());
    }

    private EnvironmentValidationDto createEnvironmentDto(String publicKey, String publicKeyId) {
        return createEnvironmentDto("AWS", false, publicKey, publicKeyId);
    }

    private EnvironmentValidationDto createEnvironmentDto(String cloudPlatform, Boolean govCloud, String publicKey, String publicKeyId) {
        Credential credential = new Credential();
        credential.setGovCloud(govCloud);
        return EnvironmentValidationDto.builder()
                .withEnvironmentDto(EnvironmentDto.builder()
                        .withCloudPlatform(cloudPlatform)
                        .withCredential(credential)
                        .withAuthentication(AuthenticationDto.builder()
                                .withPublicKey(publicKey)
                                .withPublicKeyId(publicKeyId)
                                .build())
                        .build())
                .build();
    }

}