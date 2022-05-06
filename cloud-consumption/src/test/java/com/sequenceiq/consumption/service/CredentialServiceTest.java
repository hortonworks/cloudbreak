package com.sequenceiq.consumption.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.consumption.dto.Credential;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

public class CredentialServiceTest {

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String CRN = "crn";

    private static final String NAME = "name";

    private static final String ATTRIBUTES = "attributes";

    private static final String ACCOUNT_ID = "accountId";

    private static final String CLOUD_PLATFORM = "cloudPlatform";

    private static final String OTHER_CLIENT_ERROR = "someError";

    @Mock
    private CredentialEndpoint credentialEndpoint;

    @Mock
    private SecretService secretService;

    @Mock
    private SecretResponse secretResponse;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private CredentialService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(secretService.getByResponse(secretResponse)).thenReturn(ATTRIBUTES);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn(OTHER_CLIENT_ERROR);
    }

    @Test
    public void testGetCredentialByEnvCrnSuccessful() {
        CredentialResponse credentialResponse = new CredentialResponse();
        credentialResponse.setCrn(CRN);
        credentialResponse.setName(NAME);
        credentialResponse.setAccountId(ACCOUNT_ID);
        credentialResponse.setCloudPlatform(CLOUD_PLATFORM);
        credentialResponse.setAttributes(secretResponse);
        when(credentialEndpoint.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(credentialResponse);

        Credential credential = underTest.getCredentialByEnvCrn(ENVIRONMENT_CRN);

        assertEquals(CRN, credential.getCrn());
        assertEquals(NAME, credential.getName());
        assertEquals(ACCOUNT_ID, credential.getAccountId());
        assertEquals(CLOUD_PLATFORM, credential.getCloudPlatform());
        assertEquals(ATTRIBUTES, credential.getAttributes());
    }

    @Test
    public void testCredentialNotFound() {
        Response response = Response.status(Response.Status.NOT_FOUND).build();
        when(credentialEndpoint.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenThrow(new ClientErrorException(response));

        BadRequestException result = assertThrows(BadRequestException.class, () -> underTest.getCredentialByEnvCrn(ENVIRONMENT_CRN));
        assertEquals(String.format("Credential not found by environment CRN: %s", ENVIRONMENT_CRN), result.getMessage());
    }

    @Test
    public void testOtherClientError() {
        Response response = Response.status(Response.Status.BAD_REQUEST).build();
        when(credentialEndpoint.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenThrow(new ClientErrorException(response));

        CloudbreakServiceException result = assertThrows(CloudbreakServiceException.class, () -> underTest.getCredentialByEnvCrn(ENVIRONMENT_CRN));
        assertEquals(String.format("Failed to get credential: %s", OTHER_CLIENT_ERROR), result.getMessage());
    }

    @Test
    public void testOtherError() {
        when(credentialEndpoint.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenThrow(new RuntimeException("error"));

        RuntimeException result = assertThrows(RuntimeException.class, () -> underTest.getCredentialByEnvCrn(ENVIRONMENT_CRN));
        assertEquals(result.getMessage(), "error");
    }
}
