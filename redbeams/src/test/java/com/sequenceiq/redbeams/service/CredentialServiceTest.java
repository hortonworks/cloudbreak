package com.sequenceiq.redbeams.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;

import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialResponseParameters;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.redbeams.dto.Credential;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ExtendWith(MockitoExtension.class)
class CredentialServiceTest {

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String CRN = "crn";

    private static final String NAME = "name";

    private static final String ATTRIBUTES = "attributes";

    private static final String SUBSCRIPTION_ID = "subscriptionId";

    @SuppressFBWarnings(value = "UrF", justification = "Gets injected into CredentialService")
    @Spy
    private RetryTemplate cbRetryTemplate = new RetryTemplate();

    @Mock
    private CredentialEndpoint credentialEndpoint;

    @Mock
    private SecretService secretService;

    @Mock
    private SecretResponse secretResponse;

    @InjectMocks
    private CredentialService underTest;

    @BeforeEach
    public void setUp() throws Exception {
        when(secretService.getByResponse(secretResponse)).thenReturn(ATTRIBUTES);
    }

    @Test
    void testNonAzureCredential() {
        CredentialResponse credentialResponse = new CredentialResponse();
        credentialResponse.setCrn(CRN);
        credentialResponse.setName(NAME);
        credentialResponse.setAttributes(secretResponse);
        when(credentialEndpoint.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(credentialResponse);

        Credential credential = underTest.getCredentialByEnvCrn(ENVIRONMENT_CRN);

        assertEquals(CRN, credential.getCrn());
        assertEquals(NAME, credential.getName());
        assertEquals(ATTRIBUTES, credential.getAttributes());
        assertFalse(credential.getAzure().isPresent());
    }

    @Test
    void testAzureCredential() {
        CredentialResponse credentialResponse = new CredentialResponse();
        credentialResponse.setCrn(CRN);
        credentialResponse.setName(NAME);
        credentialResponse.setAttributes(secretResponse);
        AzureCredentialResponseParameters azureResponse = new AzureCredentialResponseParameters();
        azureResponse.setSubscriptionId(SUBSCRIPTION_ID);
        credentialResponse.setAzure(azureResponse);
        when(credentialEndpoint.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(credentialResponse);

        Credential credential = underTest.getCredentialByEnvCrn(ENVIRONMENT_CRN);

        assertEquals(CRN, credential.getCrn());
        assertEquals(NAME, credential.getName());
        assertEquals(ATTRIBUTES, credential.getAttributes());
        assertTrue(credential.getAzure().isPresent());
        assertEquals(SUBSCRIPTION_ID, credential.getAzure().get().getSubscriptionId());
    }
}
