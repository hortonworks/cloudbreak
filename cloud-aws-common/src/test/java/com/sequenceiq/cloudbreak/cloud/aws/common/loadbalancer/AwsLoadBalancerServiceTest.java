package com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeregisterTargetsRequest;

@ExtendWith(MockitoExtension.class)
class AwsLoadBalancerServiceTest {

    private static final String REGION_NAME = "regionName";

    @InjectMocks
    private AwsLoadBalancerService underTest;

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private AmazonElasticLoadBalancingClient amazonElbClient;

    @Mock
    private CloudContext cloudContext;

    @Test
    void testRemoveLoadBalancerTargetsWhenTargetGroupIsEmpty() {
        underTest.removeLoadBalancerTargets(ac, emptyList(), emptyList());

        verifyNoInteractions(awsClient);
    }

    @Test
    void testRemoveLoadBalancerTargetsWhenDeregisterCalled() {
        CloudResource instanceToRemove = CloudResource.builder()
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withInstanceId("instanceId")
                .withParameters(Collections.emptyMap())
                .build();

        ArgumentCaptor<DeregisterTargetsRequest> argumentCaptor = ArgumentCaptor.forClass(DeregisterTargetsRequest.class);

        setUpAwsClient();
        underTest.removeLoadBalancerTargets(ac, List.of("targetArn"), List.of(instanceToRemove));

        verify(awsClient).createElasticLoadBalancingClient(any(), any());
        verify(amazonElbClient).deregisterTargets(argumentCaptor.capture());

        DeregisterTargetsRequest deregisterTargetsRequest = argumentCaptor.getValue();
        assertEquals("targetArn", deregisterTargetsRequest.targetGroupArn());
        assertEquals("instanceId", deregisterTargetsRequest.targets().get(0).id());
    }

    private void setUpAwsClient() {
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region(REGION_NAME)));
        when(awsClient.createElasticLoadBalancingClient(any(), any())).thenReturn(amazonElbClient);
    }
}
