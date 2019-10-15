package com.sequenceiq.environment.credential.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Set;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.validation.definition.CredentialDefinitionService;

@ExtendWith(MockitoExtension.class)
public class CredentialValidatorTest {

    private final CredentialDefinitionService credentialDefinitionService = Mockito.mock(CredentialDefinitionService.class);

    private final CredentialValidator credentialValidator
            = new CredentialValidator(Set.of("AWS", "AZURE"), credentialDefinitionService, Collections.emptyList());

    @Test
    public void testValidateCredentialUpdate() {
        Credential original = new Credential();
        original.setCloudPlatform(CloudPlatform.AWS.name());
        Credential newCred = new Credential();
        newCred.setCloudPlatform(CloudPlatform.AWS.name());

        ValidationResult result = credentialValidator.validateCredentialUpdate(original, newCred);
        assertFalse(result.hasError());
    }

    @Test
    public void testValidateCredentialUpdateInvalidPlatformChange() {
        Credential original = new Credential();
        original.setCloudPlatform(CloudPlatform.AWS.name());
        Credential newCred = new Credential();
        newCred.setCloudPlatform(CloudPlatform.AZURE.name());

        ValidationResult result = credentialValidator.validateCredentialUpdate(original, newCred);
        assertEquals(1, result.getErrors().size());
        assertThat(result.getErrors().get(0),
                CoreMatchers.containsString("CloudPlatform of the credential cannot be changed! Original: 'AWS' New: 'AZURE'."));
    }
}
