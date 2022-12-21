package com.sequenceiq.environment.credential.validation;

import static com.sequenceiq.common.model.CredentialType.AUDIT;
import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.validation.definition.CredentialDefinitionService;

@ExtendWith(MockitoExtension.class)
class CredentialValidatorTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String AWS = "AWS";

    private static final String AZURE = "AZURE";

    private static final String GCP = "GCP";

    private static final String FOO = "FOO";

    private static final String AZURE_DISABLED = " & Azure disabled";

    private static final String AZURE_ENABLED = " & Azure enabled";

    private static final String GCP_ENABLED = " & Google enabled";

    private static final String GCP_DISABLED = " & Google disabled";

    private static final String GCP_AUDIT_ENABLED = " & Google audit enabled";

    private static final String GCP_AUDIT_DISABLED = " & Google audit disabled";

    @Mock
    private CredentialDefinitionService credentialDefinitionService;

    @Mock
    private EntitlementService entitlementService;

    private CredentialValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new CredentialValidator(Set.of(AWS, AZURE, GCP), credentialDefinitionService, Collections.emptyList(), entitlementService);
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] validateCredentialCloudPlatformDataProvider() {
        return new Object[][]{
                //testCaseName             cloudPlatform   azureEnabled    gcpEnabled      gcpAuditEnabled  validExpected   credentialType
                {AWS + AZURE_DISABLED,      AWS,            false,          false,          false,              true,           ENVIRONMENT},
                {AZURE + AZURE_DISABLED,    AZURE,          false,          false,          false,              false,          ENVIRONMENT},
                {FOO + AZURE_DISABLED,      FOO,            false,          false,          false,              false,          ENVIRONMENT},
                {AWS + AZURE_ENABLED,       AWS,            true,           false,          false,              true,           ENVIRONMENT},
                {AZURE + AZURE_ENABLED,     AZURE,          true,           false,          false,              true,           ENVIRONMENT},
                {FOO + AZURE_ENABLED,       FOO,            true,           false,          false,              false,          ENVIRONMENT},
                {FOO + AZURE_ENABLED,       FOO,            true,           false,          false,              false,          ENVIRONMENT},
                {GCP + GCP_ENABLED,         GCP,            true,           true,           false,              true,           ENVIRONMENT},
                {GCP + GCP_DISABLED,        GCP,            true,           false,          false,              false,          ENVIRONMENT},
                {GCP + GCP_AUDIT_ENABLED,   GCP,            true,           true,           true,               true,           AUDIT},
                {GCP + GCP_AUDIT_DISABLED,  GCP,            true,           true,           false,              false,          AUDIT},
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("validateCredentialCloudPlatformDataProvider")
    void testValidateCredentialCloudPlatform(String testCaseName,
        String cloudPlatform,
        boolean azureEnabled,
        boolean gcpEnabled,
        boolean gcpAuditEnabled,
        boolean validExpected,
        CredentialType credentialType) {
        if (CloudPlatform.AZURE.equalsIgnoreCase(cloudPlatform)) {
            when(entitlementService.azureEnabled(ACCOUNT_ID)).thenReturn(azureEnabled);
        }
        if (CloudPlatform.GCP.equalsIgnoreCase(cloudPlatform) && credentialType.equals(AUDIT)) {
            when(entitlementService.gcpAuditEnabled(ACCOUNT_ID)).thenReturn(gcpAuditEnabled);
        }
        if (CloudPlatform.GCP.equalsIgnoreCase(cloudPlatform) && credentialType.equals(ENVIRONMENT)) {
            when(entitlementService.gcpEnabled(ACCOUNT_ID)).thenReturn(gcpEnabled);
        }
        if (validExpected) {
            underTest.validateCredentialCloudPlatform(cloudPlatform, USER_CRN, credentialType);
        } else {
            assertThrows(BadRequestException.class, () -> underTest.validateCredentialCloudPlatform(cloudPlatform, USER_CRN, credentialType));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validateCredentialCloudPlatformDataProvider")
    void testIsCredentialCloudPlatformValid(String testCaseName,
        String cloudPlatform,
        boolean azureEnabled,
        boolean gcpEnabled,
        boolean gcpAuditEnabled,
        boolean validExpected,
        CredentialType credentialType) {
        if (CloudPlatform.AZURE.equalsIgnoreCase(cloudPlatform)) {
            when(entitlementService.azureEnabled(ACCOUNT_ID)).thenReturn(azureEnabled);
        }
        if (CloudPlatform.GCP.equalsIgnoreCase(cloudPlatform) && credentialType.equals(AUDIT)) {
            when(entitlementService.gcpAuditEnabled(ACCOUNT_ID)).thenReturn(gcpAuditEnabled);
        }
        if (CloudPlatform.GCP.equalsIgnoreCase(cloudPlatform) && credentialType.equals(ENVIRONMENT)) {
            when(entitlementService.gcpEnabled(ACCOUNT_ID)).thenReturn(gcpEnabled);
        }
        assertThat(underTest.isCredentialCloudPlatformValid(cloudPlatform, ACCOUNT_ID, credentialType)).isEqualTo(validExpected);
    }

    @Test
    void testValidateCredentialUpdate() {
        Credential original = new Credential();
        original.setCloudPlatform(CloudPlatform.AWS.name());
        Credential newCred = new Credential();
        newCred.setCloudPlatform(CloudPlatform.AWS.name());

        ValidationResult result = underTest.validateCredentialUpdate(original, newCred, ENVIRONMENT);
        assertFalse(result.hasError());
    }

    @Test
    void testValidateCredentialUpdateWhenInvalidPlatformChange() {
        Credential original = new Credential();
        original.setCloudPlatform(CloudPlatform.AWS.name());
        Credential newCred = new Credential();
        newCred.setCloudPlatform(CloudPlatform.AZURE.name());

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
        request.setCloudPlatform("AWS");
        ValidationResult result = underTest.validateAwsCredentialRequest(request);
        assertTrue(result.hasError());
        assertEquals("Role ARN is not found in credential request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsCredentialRequestKeyBased() {
        CredentialRequest request = new CredentialRequest();
        request.setCloudPlatform("AWS");
        request.setAws(new AwsCredentialParameters());
        ValidationResult result = underTest.validateAwsCredentialRequest(request);
        assertTrue(result.hasError());
        assertEquals("Role ARN is not found in credential request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsCredentialRequestNoArn() {
        CredentialRequest request = new CredentialRequest();
        request.setCloudPlatform("AWS");
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
        request.setCloudPlatform("AWS");
        AwsCredentialParameters aws = new AwsCredentialParameters();
        RoleBasedParameters roleBased = new RoleBasedParameters();
        roleBased.setRoleArn("arn");
        aws.setRoleBased(roleBased);
        request.setAws(aws);
        ValidationResult result = underTest.validateAwsCredentialRequest(request);
        assertFalse(result.hasError());
    }

}
