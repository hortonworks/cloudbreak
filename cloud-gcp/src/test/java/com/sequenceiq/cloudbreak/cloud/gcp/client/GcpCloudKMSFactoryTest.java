package com.sequenceiq.cloudbreak.cloud.gcp.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@ExtendWith(MockitoExtension.class)
public class GcpCloudKMSFactoryTest {

    @Mock
    private JsonFactory jsonFactory;

    @Mock
    private GcpCredentialFactory gcpCredentialFactory;

    @Mock
    private HttpTransport httpTransport;

    @InjectMocks
    private GcpCloudKMSFactory underTest;

    @Test
    public void testBuildCloudKMSWhenCreateScopedRequiredTrueShouldCallCreateScopedMethod() throws IOException, GeneralSecurityException {
        CloudCredential cloudCredential = mock(CloudCredential.class);
        GoogleCredential googleCredential = mock(GoogleCredential.class);
        when(gcpCredentialFactory.buildCredential(any(CloudCredential.class), any(HttpTransport.class)))
                .thenReturn(googleCredential);
        when(googleCredential.createScopedRequired()).thenReturn(true);
        when(cloudCredential.getName()).thenReturn("name");
        underTest.buildCloudKMS(cloudCredential);
        verify(googleCredential, times(1)).createScoped(any());
    }

    @Test
    public void testBuildCloudKMSWhenCreateScopedRequiredFalseShouldNOTCallCreateScopedMethod() throws IOException, GeneralSecurityException {
        CloudCredential cloudCredential = mock(CloudCredential.class);
        GoogleCredential googleCredential = mock(GoogleCredential.class);
        when(gcpCredentialFactory.buildCredential(any(CloudCredential.class), any(HttpTransport.class)))
                .thenReturn(googleCredential);
        when(googleCredential.createScopedRequired()).thenReturn(false);
        when(cloudCredential.getName()).thenReturn("name");
        underTest.buildCloudKMS(cloudCredential);
        verify(googleCredential, times(0)).createScoped(any());
    }
}