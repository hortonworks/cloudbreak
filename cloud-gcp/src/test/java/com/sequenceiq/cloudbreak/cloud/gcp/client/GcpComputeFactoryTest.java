package com.sequenceiq.cloudbreak.cloud.gcp.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@ExtendWith(MockitoExtension.class)
public class GcpComputeFactoryTest {

    @Mock
    private JsonFactory jsonFactory;

    @Mock
    private GcpCredentialFactory gcpCredentialFactory;

    @Mock
    private HttpTransport httpTransport;

    @InjectMocks
    private GcpComputeFactory underTest;

    @Test
    public void testBuildComputeWhenNoError() throws IOException, GeneralSecurityException {
        CloudCredential cloudCredential = mock(CloudCredential.class);
        GoogleCredential googleCredential = mock(GoogleCredential.class);
        when(gcpCredentialFactory.buildCredential(any(CloudCredential.class), any(HttpTransport.class)))
                .thenReturn(googleCredential);
        when(cloudCredential.getName()).thenReturn("name");
        underTest.buildCompute(cloudCredential);
    }

    @Test
    public void testBuildComputeWhenErrorComesShouldThrowCredentialVerificationException() throws IOException, GeneralSecurityException {
        CloudCredential cloudCredential = mock(CloudCredential.class);
        doThrow(new GeneralSecurityException("error"))
                .when(gcpCredentialFactory).buildCredential(any(CloudCredential.class), any(HttpTransport.class));
        Assertions.assertThrows(CredentialVerificationException.class, () -> underTest.buildCompute(cloudCredential));
    }
}