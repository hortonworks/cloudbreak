package com.sequenceiq.cloudbreak.cloud.gcp.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.junit.Assert;
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
public class GcpStorageFactoryTest {

    @Mock
    private JsonFactory jsonFactory;

    @Mock
    private GcpCredentialFactory gcpCredentialFactory;

    @Mock
    private HttpTransport httpTransport;

    @InjectMocks
    private GcpStorageFactory underTest;

    @Test
    public void testBuildGcpStorageFactoryWhenNoError() throws IOException, GeneralSecurityException {
        CloudCredential cloudCredential = mock(CloudCredential.class);
        GoogleCredential googleCredential = mock(GoogleCredential.class);
        when(gcpCredentialFactory.buildCredential(any(CloudCredential.class), any(HttpTransport.class)))
                .thenReturn(googleCredential);
        underTest.buildStorage(cloudCredential, "name");
    }

    @Test
    public void testBuildGcpStorageFactoryWhenErrorComesShouldThrowCredentialVerificationException() throws IOException, GeneralSecurityException {
        CloudCredential cloudCredential = mock(CloudCredential.class);
        doThrow(new GeneralSecurityException("error"))
                .when(gcpCredentialFactory).buildCredential(any(CloudCredential.class), any(HttpTransport.class));
        Assert.assertNull(underTest.buildStorage(cloudCredential, "name"));
    }
}