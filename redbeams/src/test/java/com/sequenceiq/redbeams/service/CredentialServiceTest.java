package com.sequenceiq.redbeams.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialResponseParameters;
import com.sequenceiq.redbeams.dto.Credential;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class CredentialServiceTest {

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String CRN = "crn";

    private static final String NAME = "name";

    private static final String ATTRIBUTES = "attributes";

    private static final String SUBSCRIPTION_ID = "subscriptionId";

    @Mock
    private CredentialEndpoint credentialEndpoint;

    @Mock
    private SecretService secretService;

    @Mock
    private SecretResponse secretResponse;

    @InjectMocks
    private CredentialService underTest;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(secretService.getByResponse(secretResponse)).thenReturn(ATTRIBUTES);
    }

    @Test
    public void testNonAzureCredential() {
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
    public void testAzureCredential() {
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
