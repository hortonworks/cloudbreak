package com.sequenceiq.cloudbreak.cloud.gcp.client;

import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

@ExtendWith(MockitoExtension.class)
public class GcpServiceFactoryTest {

    private TestGcpServiceFactory underTest = new TestGcpServiceFactory();

    @Test
    public void testRequestInitializer() throws IOException {
        GoogleCredential credential = mock(GoogleCredential.class);
        underTest.requestInitializer(credential);
    }

}