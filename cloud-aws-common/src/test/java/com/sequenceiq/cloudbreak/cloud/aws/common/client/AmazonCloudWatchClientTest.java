package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.TagResourceRequest;
import software.amazon.awssdk.services.cloudwatch.model.TagResourceResponse;

@ExtendWith(MockitoExtension.class)
class AmazonCloudWatchClientTest {

    @Mock
    private CloudWatchClient client;

    @InjectMocks
    private AmazonCloudWatchClient underTest;

    @Test
    void testTagResource() {
        TagResourceRequest request = mock(TagResourceRequest.class);
        TagResourceResponse expectedResponse = mock(TagResourceResponse.class);
        when(client.tagResource(request)).thenReturn(expectedResponse);

        TagResourceResponse response = underTest.tagResource(request);

        assertEquals(expectedResponse, response);
    }
}