package com.sequenceiq.cloudbreak.service.environment.credential;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

@ExtendWith(MockitoExtension.class)
class CredentialClientServiceTest {

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    @Mock
    private CredentialEndpoint credentialEndpoint;

    @Mock
    private CredentialConverter credentialConverter;

    @Spy
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private CredentialToExtendedCloudCredentialConverter credentialToExtendedCloudCredentialConverter;

    @InjectMocks
    private CredentialClientService underTest;

    @Test
    void testGetCloudCredential() {
        CredentialResponse credentialResponse = new CredentialResponse();
        when(credentialEndpoint.getByEnvironmentCrn(eq(ENVIRONMENT_CRN))).thenReturn(credentialResponse);
        Credential credential = Credential.builder().crn("crn").name("name").account("account").attributes(new Json(new HashMap<>())).build();
        when(credentialConverter.convert(eq(credentialResponse))).thenReturn(credential);

        CloudCredential cloudCredential = underTest.getCloudCredential(ENVIRONMENT_CRN);

        verify(credentialEndpoint, times(1)).getByEnvironmentCrn(eq(ENVIRONMENT_CRN));
        assertEquals("crn", cloudCredential.getId());
        assertEquals("name", cloudCredential.getName());
        assertEquals("account", cloudCredential.getAccountId());
    }

    @Test
    void testExtendedCloudCredential() {
        CredentialResponse credentialResponse = new CredentialResponse();
        when(credentialEndpoint.getByEnvironmentCrn(eq(ENVIRONMENT_CRN))).thenReturn(credentialResponse);
        ExtendedCloudCredential extendedCloudCredential = mock(ExtendedCloudCredential.class);
        when(credentialToExtendedCloudCredentialConverter.convert(any())).thenReturn(extendedCloudCredential);

        ExtendedCloudCredential response = underTest.getExtendedCloudCredential(ENVIRONMENT_CRN);
        assertEquals(extendedCloudCredential, response);
        verify(credentialToExtendedCloudCredentialConverter, times(1)).convert(any());
    }
}