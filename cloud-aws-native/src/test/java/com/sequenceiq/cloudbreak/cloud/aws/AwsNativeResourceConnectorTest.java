package com.sequenceiq.cloudbreak.cloud.aws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerService;
import com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer.AwsNativeLoadBalancerLaunchService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.template.init.ContextBuilders;
import com.sequenceiq.cloudbreak.cloud.template.network.NetworkResourceService;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class AwsNativeResourceConnectorTest {

    private static final String REGION_NAME = "regionName";

    private static final String AZ = "AZ";

    @InjectMocks
    private AwsNativeResourceConnector underTest;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private CloudLoadBalancer cloudLoadBalancer;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private AdjustmentTypeWithThreshold adjustmentTypeWithThreshold;

    @Mock
    private AwsContext awsContext;

    @Mock
    private CommonAwsClient commonAwsClient;

    @Mock
    private AwsNativeLoadBalancerLaunchService loadBalancerLaunchService;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private LoadBalancerService loadBalancerService;

    @Mock
    private AmazonElasticLoadBalancingClient elasticLoadBalancingClient;

    @Mock
    private ContextBuilders contextBuilders;

    @Mock
    private ResourceContextBuilder resourceContextBuilder;

    @Mock
    private NetworkResourceService networkResourceService;

    @Mock
    private ComputeResourceService computeResourceService;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Test
    public void testUpscale() throws QuotaExceededException {
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(contextBuilders.get(any())).thenReturn(resourceContextBuilder);
        when(resourceContextBuilder.contextInit(any(), any(), any(), anyBoolean())).thenReturn(awsContext);
        when(networkResourceService.getNetworkResources(any(), any())).thenReturn(Collections.emptyList());
        when(commonAwsClient.createElasticLoadBalancingClient(any(), any())).thenReturn(elasticLoadBalancingClient);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region(REGION_NAME), AvailabilityZone.availabilityZone(AZ)));

        underTest.upscale(ac, cloudStack, Collections.emptyList(), adjustmentTypeWithThreshold);

        verify(loadBalancerLaunchService).launchLoadBalancerResources(ac, cloudStack, persistenceNotifier, elasticLoadBalancingClient, true);
    }

    @Test
    public void testDownscale() {
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getId()).thenReturn(0L);
        when(contextBuilders.get(any())).thenReturn(resourceContextBuilder);
        when(resourceContextBuilder.contextInit(any(), any(), any(), anyBoolean())).thenReturn(awsContext);
        when(resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, 0L))
                .thenReturn(Collections.emptyList());

        underTest.downscale(ac, cloudStack, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        verify(loadBalancerService).removeLoadBalancerTargets(ac, Collections.emptyList(), Collections.emptyList());
    }
}
