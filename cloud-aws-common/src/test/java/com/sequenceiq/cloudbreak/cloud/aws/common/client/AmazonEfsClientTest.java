package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.service.retry.Retry;

import software.amazon.awssdk.services.efs.EfsClient;
import software.amazon.awssdk.services.efs.model.TagResourceRequest;
import software.amazon.awssdk.services.efs.model.TagResourceResponse;

@ExtendWith(MockitoExtension.class)
class AmazonEfsClientTest {

    @Mock
    private EfsClient client;

    @Mock
    private Retry retry;

    @InjectMocks
    private AmazonEfsClient underTest;

    @Test
    void testTagResource() {
        TagResourceRequest request = mock(TagResourceRequest.class);
        TagResourceResponse expectedResponse = mock(TagResourceResponse.class);
        when(client.tagResource(request)).thenReturn(expectedResponse);
        when(retry.testWith2SecDelayMax15Times(any())).thenAnswer(
            invocation -> {
                Supplier<TagResourceResponse> supplier = invocation.getArgument(0, Supplier.class);
                return supplier.get();
            });

        TagResourceResponse response = underTest.tagResource(request);

        assertEquals(expectedResponse, response);
        verify(client).tagResource(request);
    }
}