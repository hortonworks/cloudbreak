package com.sequenceiq.cloudbreak.cloud.gcp.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequest;
import com.sequenceiq.cloudbreak.cloud.gcp.tracing.GcpTracingInterceptor;

@ExtendWith(MockitoExtension.class)
public class GcpServiceFactoryTest {

    private TestGcpServiceFactory underTest = new TestGcpServiceFactory();

    private GcpTracingInterceptor gcpTracingInterceptor = mock(GcpTracingInterceptor.class);

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(underTest, "gcpTracingInterceptor", gcpTracingInterceptor);
    }

    @Test
    public void testRequestInitializer() throws IOException {
        GoogleCredential credential = mock(GoogleCredential.class);
        doNothing().when(gcpTracingInterceptor).intercept(any(HttpRequest.class));
        underTest.requestInitializer(credential);
    }

}