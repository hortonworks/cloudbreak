package com.sequenceiq.consumption.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.consumption.converter.CredentialResponseToCloudCredentialConverter;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

@ExtendWith(MockitoExtension.class)
public class CredentialServiceTest {

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String CRN = "crn";

    private static final String NAME = "name";

    private static final String ATTRIBUTES = "attributes";

    private static final String ACCOUNT_ID = "accountId";

    private static final String OTHER_CLIENT_ERROR = "someError";

    @Mock
    private CredentialEndpoint credentialEndpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Mock
    private CredentialResponseToCloudCredentialConverter credentialResponseToCloudCredentialConverter;

    @InjectMocks
    private CredentialService underTest;

    @Test
    public void testGetCredentialByEnvCrnSuccessful() {
        CredentialResponse credentialResponse = new CredentialResponse();
        credentialResponse.setCrn(CRN);
        credentialResponse.setName(NAME);
        credentialResponse.setAccountId(ACCOUNT_ID);
        when(credentialEndpoint.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(credentialResponse);
        when(credentialResponseToCloudCredentialConverter.convert(eq(credentialResponse))).thenReturn(new CloudCredential(CRN, NAME, ACCOUNT_ID));

        CloudCredential credential = underTest.getCloudCredentialByEnvCrn(ENVIRONMENT_CRN);

        assertEquals(CRN, credential.getId());
        assertEquals(NAME, credential.getName());
        assertEquals(ACCOUNT_ID, credential.getAccountId());
    }

    @Test
    public void testCredentialNotFound() {
        Response response = Response.status(Response.Status.NOT_FOUND).build();
        when(credentialEndpoint.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenThrow(new ClientErrorException(response));

        BadRequestException result = assertThrows(BadRequestException.class, () -> underTest.getCloudCredentialByEnvCrn(ENVIRONMENT_CRN));
        assertEquals(String.format("Credential not found by environment CRN: %s", ENVIRONMENT_CRN), result.getMessage());
    }

    @Test
    public void testOtherClientError() {
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn(OTHER_CLIENT_ERROR);
        Response response = Response.status(Response.Status.BAD_REQUEST).build();
        when(credentialEndpoint.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenThrow(new ClientErrorException(response));

        CloudbreakServiceException result = assertThrows(CloudbreakServiceException.class, () -> underTest.getCloudCredentialByEnvCrn(ENVIRONMENT_CRN));
        assertEquals(String.format("Failed to get credential: %s", OTHER_CLIENT_ERROR), result.getMessage());
    }

    @Test
    public void testOtherError() {
        when(credentialEndpoint.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenThrow(new RuntimeException("error"));

        RuntimeException result = assertThrows(RuntimeException.class, () -> underTest.getCloudCredentialByEnvCrn(ENVIRONMENT_CRN));
        assertEquals(result.getMessage(), "error");
    }
}
