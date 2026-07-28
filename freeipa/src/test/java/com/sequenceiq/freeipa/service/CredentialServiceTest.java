package com.sequenceiq.freeipa.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.freeipa.dto.Credential;

@ExtendWith(MockitoExtension.class)
class CredentialServiceTest {

    private static final String ENVIRONMENT_CRN = "envCrn";

    @Mock
    private CredentialEndpoint credentialEndpoint;

    @Mock
    private SecretService secretService;

    @InjectMocks
    private CredentialService underTest;

    @Test
    void testGetCredentialByEnvCrnShouldPropagateGovCloudTrue() {
        when(credentialEndpoint.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(credentialResponse(true));
        when(secretService.getByResponse(any())).thenReturn("{}");

        Credential result = underTest.getCredentialByEnvCrn(ENVIRONMENT_CRN);

        assertTrue(result.isGovCloud());
    }

    @Test
    void testGetCredentialByEnvCrnShouldDefaultGovCloudToFalseWhenResponseHasNull() {
        when(credentialEndpoint.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(credentialResponse(null));
        when(secretService.getByResponse(any())).thenReturn("{}");

        Credential result = underTest.getCredentialByEnvCrn(ENVIRONMENT_CRN);

        assertFalse(result.isGovCloud());
    }

    private CredentialResponse credentialResponse(Boolean govCloud) {
        CredentialResponse response = new CredentialResponse();
        response.setName("name");
        response.setCrn("crn");
        response.setAccountId("account");
        response.setGovCloud(govCloud);
        return response;
    }
}
