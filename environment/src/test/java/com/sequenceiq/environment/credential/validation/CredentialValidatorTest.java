package com.sequenceiq.environment.credential.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

}
