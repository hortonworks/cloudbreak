package com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeregisterTargetsRequest;

@ExtendWith(MockitoExtension.class)
public class LoadBalancerServiceTest {

    private static final String REGION_NAME = "regionName";

    private static final String AZ = "AZ";

    @InjectMocks
    private LoadBalancerService underTest;

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private CloudLoadBalancer loadBalancer;

    @Mock
    private AmazonElasticLoadBalancingClient amazonElbClient;

    @Mock
    private CloudContext cloudContext;

    @Test
    public void testRemoveLoadBalancerTargetsWhenTargetGroupIsEmpty() {
        underTest.removeLoadBalancerTargets(ac, emptyList(), emptyList());

        verify(awsClient, never()).createElasticLoadBalancingClient(any(), any());
    }

    @Test
    public void testRemoveLoadBalancerTargetsWhenDeregisterCalled() {
        CloudResource instanceToRemove = CloudResource.builder()
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withInstanceId("instanceId")
                .withParameters(Collections.emptyMap())
                .build();

        ArgumentCaptor<DeregisterTargetsRequest> argumentCaptor = ArgumentCaptor.forClass(DeregisterTargetsRequest.class);

        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region(REGION_NAME), AvailabilityZone.availabilityZone(AZ)));
        when(awsClient.createElasticLoadBalancingClient(any(), any())).thenReturn(amazonElbClient);
        underTest.removeLoadBalancerTargets(ac, List.of("targetArn"), List.of(instanceToRemove));

        verify(awsClient).createElasticLoadBalancingClient(any(), any());
        verify(amazonElbClient).deregisterTargets(argumentCaptor.capture());

        DeregisterTargetsRequest deregisterTargetsRequest = argumentCaptor.getValue();
        assertEquals("targetArn", deregisterTargetsRequest.targetGroupArn());
        assertEquals("instanceId", deregisterTargetsRequest.targets().get(0).id());
    }
}
