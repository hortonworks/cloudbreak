package com.sequenceiq.environment.credential.validation;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.validation.definition.CredentialDefinitionService;

@ExtendWith(MockitoExtension.class)
class CredentialValidatorTest {

    private static final String FOO = "FOO";

    private static final String AZURE_DISABLED = " & Azure disabled";

    private static final String AZURE_ENABLED = " & Azure enabled";

    private static final String GCP_AUDIT_ENABLED = " & Google audit enabled";

    @Mock
    private CredentialDefinitionService credentialDefinitionService;

    @Mock
    private ProviderCredentialValidator providerCredentialValidator;

    private CredentialValidator underTest;

    @BeforeEach
    void setUp() {
        when(providerCredentialValidator.supportedProvider()).thenReturn(AWS.name());
        underTest = new CredentialValidator(Set.of(AWS.name(), AZURE.name(), GCP.name()), credentialDefinitionService, List.of(providerCredentialValidator));
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] validateCredentialCloudPlatformDataProvider() {
        return new Object[][]{
                //testCaseName             cloudPlatform    validExpected   credentialType
                {AWS.name() + AZURE_DISABLED,       AWS.name(),        true},
                {AZURE.name() + AZURE_DISABLED,     AZURE.name(),      true},
                {FOO + AZURE_DISABLED,              FOO,               false},
                {FOO + AZURE_ENABLED,               FOO,               false},
                {GCP.name() + GCP_AUDIT_ENABLED,    GCP.name(),        true},
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("validateCredentialCloudPlatformDataProvider")
    void testValidateCredentialCloudPlatform(String testCaseName,
            String cloudPlatform,
            boolean validExpected) {
        if (validExpected) {
            underTest.validateCredentialCloudPlatform(cloudPlatform);
        } else {
            assertThrows(BadRequestException.class, () -> underTest.validateCredentialCloudPlatform(cloudPlatform));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validateCredentialCloudPlatformDataProvider")
    void testIsCredentialCloudPlatformValid(String testCaseName,
            String cloudPlatform,
            boolean validExpected) {
        assertThat(underTest.isCredentialCloudPlatformValid(cloudPlatform)).isEqualTo(validExpected);
    }

    @Test
    void testValidateCreateWhenNoError() {
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setCloudPlatform(AWS.name());

        assertDoesNotThrow(() -> underTest.validateCreate(credentialRequest));
    }

    @Test
    void testValidateCreateWhenError() {
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setCloudPlatform(AWS.name());

        when(providerCredentialValidator.validateCreate(any(), any())).thenReturn(ValidationResult.builder().error("error").build());
        BadRequestException exc = assertThrows(BadRequestException.class, () -> underTest.validateCreate(credentialRequest));
        assertEquals("error", exc.getMessage());
    }

    @Test
    void testValidateCredentialUpdate() {
        Credential original = new Credential();
        original.setCloudPlatform(AWS.name());
        Credential newCred = new Credential();
        newCred.setCloudPlatform(AWS.name());

        ValidationResult result = underTest.validateCredentialUpdate(original, newCred, ENVIRONMENT);
        assertFalse(result.hasError());
    }

    @Test
    void testValidateCredentialUpdateWhenInvalidPlatformChange() {
        Credential original = new Credential();
        original.setCloudPlatform(AWS.name());
        Credential newCred = new Credential();
        newCred.setCloudPlatform(AZURE.name());

        ValidationResult result = underTest.validateCredentialUpdate(original, newCred, ENVIRONMENT);
        assertEquals(1, result.getErrors().size());
        assertThat(result.getErrors().get(0)).contains("CloudPlatform of the credential cannot be changed! Original: 'AWS' New: 'AZURE'.");
    }

    @Test
    void testValidateAwsCredentialRequestNotAWS() {
        CredentialRequest request = new CredentialRequest();
        request.setCloudPlatform("AZURE");
        ValidationResult result = underTest.validateAwsCredentialRequest(request);
        assertTrue(result.hasError());
        assertEquals("Credential request is not for AWS.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsCredentialRequestNoAwsParams() {
        CredentialRequest request = new CredentialRequest();
        request.setCloudPlatform(AWS.name());
        ValidationResult result = underTest.validateAwsCredentialRequest(request);
        assertTrue(result.hasError());
        assertEquals("Role ARN is not found in credential request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsCredentialRequestKeyBased() {
        CredentialRequest request = new CredentialRequest();
        request.setCloudPlatform(AWS.name());
        request.setAws(new AwsCredentialParameters());
        ValidationResult result = underTest.validateAwsCredentialRequest(request);
        assertTrue(result.hasError());
        assertEquals("Role ARN is not found in credential request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsCredentialRequestNoArn() {
        CredentialRequest request = new CredentialRequest();
        request.setCloudPlatform(AWS.name());
        AwsCredentialParameters aws = new AwsCredentialParameters();
        aws.setRoleBased(new RoleBasedParameters());
        request.setAws(aws);
        ValidationResult result = underTest.validateAwsCredentialRequest(request);
        assertTrue(result.hasError());
        assertEquals("Role ARN is not found in credential request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsCredentialRequestValid() {
        CredentialRequest request = new CredentialRequest();
        request.setCloudPlatform(AWS.name());
        AwsCredentialParameters aws = new AwsCredentialParameters();
        RoleBasedParameters roleBased = new RoleBasedParameters();
        roleBased.setRoleArn("arn");
        aws.setRoleBased(roleBased);
        request.setAws(aws);
        ValidationResult result = underTest.validateAwsCredentialRequest(request);
        assertFalse(result.hasError());
    }

    @Test
    void testGetValidPlatformsWhenAllEnabledAndEnvironmentCredential() {
        Set<String> expectedEnabledPlatforms = Set.of(AWS.name(), AZURE.name(), GCP.name());

        Set<String> result = underTest.getValidPlatforms();
        assertEquals(expectedEnabledPlatforms.size(), result.size());
        assertTrue(result.containsAll(expectedEnabledPlatforms));
    }

}
