package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.FQDN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerService;
import com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer.AwsNativeLoadBalancerLaunchService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder;
import com.sequenceiq.cloudbreak.cloud.template.authentication.AuthenticationResourceService;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.template.group.GroupResourceService;
import com.sequenceiq.cloudbreak.cloud.template.init.ContextBuilders;
import com.sequenceiq.cloudbreak.cloud.template.network.NetworkResourceService;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
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
    private AuthenticationResourceService authenticationResourceService;

    @Mock
    private ComputeResourceService computeResourceService;

    @Mock
    private CloudResourceHelper cloudResourceHelper;

    @Mock
    private GroupResourceService groupResourceService;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Test
    public void testUpscale() throws QuotaExceededException {
        CloudResource instance1CloudResource = CloudResource.builder().withName("instance1").withType(ResourceType.AWS_INSTANCE).build();
        CloudResource instance2CloudResource = CloudResource.builder().withName("instance2").withType(ResourceType.AWS_INSTANCE).build();
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az1", true, "fstab", null, null, null);
        volumeSetAttributes.setDiscoveryFQDN("fqdn1");
        CloudResource volumeSet1 = CloudResource.builder().withName("volumeset1").withType(ResourceType.AWS_VOLUMESET)
                .withStatus(CommonStatus.DETACHED).withGroup("master").withParameters(Map.of(CloudResource.ATTRIBUTES,
                        volumeSetAttributes))
                .withInstanceId("i-1").build();
        CloudResource volumeSet2 = CloudResource.builder().withName("volumeset2").withType(ResourceType.AWS_VOLUMESET)
                .withStatus(CommonStatus.DETACHED).withGroup("master").withParameters(Map.of(CloudResource.ATTRIBUTES,
                        volumeSetAttributes))
                .withInstanceId("i-2").build();
        List<CloudResource> resources = List.of(instance1CloudResource, instance2CloudResource, volumeSet1, volumeSet2);

        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(authenticationResourceService.getAuthenticationResources(any(), any())).thenReturn(Collections.emptyList());
        when(contextBuilders.get(any())).thenReturn(resourceContextBuilder);
        when(resourceContextBuilder.contextInit(any(), any(), any(), anyBoolean())).thenReturn(awsContext);
        when(networkResourceService.getNetworkResources(any(), any())).thenReturn(Collections.emptyList());
        ArrayList<Group> groups = new ArrayList<>();
        Group group1 = mock(Group.class);
        List<CloudInstance> greoup1Instances = new ArrayList<>();
        InstanceAuthentication authentication = new InstanceAuthentication("pub", "id", "cloudbreak");
        CloudInstance cloudInstance = new CloudInstance("instance1",
                new InstanceTemplate("m1", "master", 1L,
                        List.of(new Volume("/xvda", "gp3", 100, CloudVolumeUsageType.GENERAL)),
                        InstanceStatus.CREATE_REQUESTED, null, 1L, null, TemporaryStorage.EPHEMERAL_VOLUMES, 1L),
                authentication, null, null, Map.of(FQDN, "fqdn1"));
        greoup1Instances.add(cloudInstance);
        when(group1.getInstances()).thenReturn(greoup1Instances);
        when(group1.getInstanceAuthentication()).thenReturn(authentication);
        when(group1.getName()).thenReturn("master");
        Group group2 = mock(Group.class);
        groups.add(group1);
        groups.add(group2);
        when(cloudStack.getGroups()).thenReturn(groups);

        underTest.upscale(ac, cloudStack, resources, adjustmentTypeWithThreshold);

        verifyNoInteractions(loadBalancerLaunchService);
        InOrder inOrder = Mockito.inOrder(cloudResourceHelper);
        ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
        inOrder.verify(cloudResourceHelper, times(1)).updateDeleteOnTerminationFlag(listArgumentCaptor.capture(), eq(false), eq(cloudContext));
        List<CloudResource> volumeResourcesDeleteOnTerminationFlagSetToFalse = listArgumentCaptor.getValue();
        assertThat(volumeResourcesDeleteOnTerminationFlagSetToFalse).extracting("name").containsExactly("volumeset1", "volumeset2");
        inOrder.verify(cloudResourceHelper, times(1)).updateDeleteOnTerminationFlag(listArgumentCaptor.capture(), eq(true), eq(cloudContext));
        List<CloudResource> volumeResourcesDeleteOnTerminationFlagSetToTrue = listArgumentCaptor.getValue();
        assertThat(volumeResourcesDeleteOnTerminationFlagSetToTrue).extracting("name").containsExactly("volumeset1", "volumeset2");
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
