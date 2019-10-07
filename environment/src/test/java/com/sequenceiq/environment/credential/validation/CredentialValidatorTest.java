package com.sequenceiq.environment.credential.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.BadRequestException;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.validation.definition.CredentialDefinitionService;

@ExtendWith(MockitoExtension.class)
class CredentialValidatorTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    @Mock
    private CredentialDefinitionService credentialDefinitionService;

    @Mock
    private EntitlementService entitlementService;

    private CredentialValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new CredentialValidator(Set.of("AWS", "AZURE"), credentialDefinitionService, Collections.emptyList(), entitlementService);
    }

    @Test
    void testValidateCredentialCloudPlatformWhenBadPlatform() {
        assertThrows(BadRequestException.class, () -> underTest.validateCredentialCloudPlatform("FOO", USER_CRN));
    }

    @Test
    void testValidateCredentialCloudPlatformWhenAzureDisabled() {
        when(entitlementService.azureEnabled(USER_CRN, ACCOUNT_ID)).thenReturn(false);
        assertThrows(BadRequestException.class, () -> underTest.validateCredentialCloudPlatform("AZURE", USER_CRN));
    }

    @Test
    void testValidateCredentialCloudPlatformWhenAzureSuccess() {
        when(entitlementService.azureEnabled(USER_CRN, ACCOUNT_ID)).thenReturn(true);
        underTest.validateCredentialCloudPlatform("AZURE", USER_CRN);
    }

    @Test
    void testValidateCredentialUpdate() {
        Credential original = new Credential();
        original.setCloudPlatform(CloudPlatform.AWS.name());
        Credential newCred = new Credential();
        newCred.setCloudPlatform(CloudPlatform.AWS.name());

        ValidationResult result = underTest.validateCredentialUpdate(original, newCred);
        assertFalse(result.hasError());
    }

    @Test
    void testValidateCredentialUpdateWhenInvalidPlatformChange() {
        Credential original = new Credential();
        original.setCloudPlatform(CloudPlatform.AWS.name());
        Credential newCred = new Credential();
        newCred.setCloudPlatform(CloudPlatform.AZURE.name());

        ValidationResult result = underTest.validateCredentialUpdate(original, newCred);
        assertEquals(1, result.getErrors().size());
        assertThat(result.getErrors().get(0),
                CoreMatchers.containsString("CloudPlatform of the credential cannot be changed! Original: 'AWS' New: 'AZURE'."));
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
