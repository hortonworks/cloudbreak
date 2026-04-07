package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.AddTagsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.AddTagsResponse;

@ExtendWith(MockitoExtension.class)
class AmazonElasticLoadBalancingClientTest {

    @Mock
    private ElasticLoadBalancingV2Client client;

    @InjectMocks
    private AmazonElasticLoadBalancingClient underTest;

    @Test
    void testAddTags() {
        AddTagsRequest request = mock(AddTagsRequest.class);
        AddTagsResponse expectedResponse = mock(AddTagsResponse.class);
        when(client.addTags(request)).thenReturn(expectedResponse);

        AddTagsResponse response = underTest.addTags(request);

        assertEquals(expectedResponse, response);
    }
}