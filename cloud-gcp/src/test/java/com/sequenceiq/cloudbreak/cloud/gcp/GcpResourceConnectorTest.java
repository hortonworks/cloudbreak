package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.common.api.type.ResourceType.GCP_ATTACHED_DISKSET;
import static com.sequenceiq.common.api.type.ResourceType.GCP_INSTANCE;
import static com.sequenceiq.common.api.type.ResourceType.GCP_RESERVED_IP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder;
import com.sequenceiq.cloudbreak.cloud.template.authentication.AuthenticationResourceService;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.group.GroupResourceService;
import com.sequenceiq.cloudbreak.cloud.template.init.ContextBuilders;
import com.sequenceiq.cloudbreak.cloud.template.loadbalancer.LoadBalancerResourceService;
import com.sequenceiq.cloudbreak.cloud.template.network.NetworkResourceService;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class GcpResourceConnectorTest {

    @InjectMocks
    private GcpResourceConnector underTest;

    @Mock
    private AuthenticationResourceService authenticationResourceService;

    @Mock
    private NetworkResourceService networkResourceService;

    @Mock
    private GroupResourceService groupResourceService;

    @Mock
    private ComputeResourceService computeResourceService;

    @Mock
    private LoadBalancerResourceService loadBalancerResourceService;

    @Mock
    private ContextBuilders contextBuilders;

    @Mock
    private CloudResourceHelper cloudResourceHelper;

    @Test
    public void testGetDBStackTemplateWhenEverythingIsFine() throws TemplatingNotSupportedException {
        assertTrue(underTest.getDBStackTemplate(null).equals(""));
    }

    @Test
    public void testGetStackTemplateWhenEverythingIsFine() {
        assertThrows(TemplatingNotSupportedException.class, () -> underTest.getStackTemplate());
    }

    @Test
    public void testGetTlsInfoWhenEverythingIsfine() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        assertEquals(false, underTest.getTlsInfo(authenticatedContext,  cloudStack).isUsePrivateIpToTls());
    }

    @Test
    public void testLaunchLoadBalancersWhenEverythingIsFine() throws Exception {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        Network network = mock(Network.class);
        CloudContext cloudContext = mock(CloudContext.class);
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        ResourceContextBuilder resourceContextBuilder = mock(ResourceContextBuilder.class);

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudStack.getNetwork()).thenReturn(network);
        when(cloudContext.getPlatform()).thenReturn(platform("GCP"));
        when(contextBuilders.get(any(Platform.class))).thenReturn(resourceContextBuilder);
        when(resourceContextBuilder.contextInit(
                any(CloudContext.class),
                any(AuthenticatedContext.class),
                any(Network.class),
                anyBoolean())).thenReturn(resourceBuilderContext);

        when(loadBalancerResourceService.buildResources(
                any(ResourceBuilderContext.class),
                any(AuthenticatedContext.class),
                any(CloudStack.class))
        ).thenReturn(List.of());

        underTest.launchLoadBalancers(authenticatedContext, cloudStack, persistenceNotifier);

        verify(contextBuilders, times(1)).get(any(Platform.class));
        verify(loadBalancerResourceService, times(1)).buildResources(
                any(ResourceBuilderContext.class),
                any(AuthenticatedContext.class),
                any(CloudStack.class));
    }

    @Test
    public void testCheckWhenEverythingIsFine() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);

        assertEquals(0, underTest.check(authenticatedContext, new ArrayList<>()).size());
    }

    @Test
    public void testCollectProviderSpecificResourcesWhenEverythingIsFine() {
        List<CloudResource> cloudResourceList = new ArrayList<>();
        cloudResourceList.add(cloudResource("test-1", GCP_RESERVED_IP));
        List<CloudInstance> cloudInstances = new ArrayList<>();
        cloudInstances.add(cloudInstance("test-1"));
        cloudInstances.add(cloudInstance("test-2"));

        assertEquals(2, underTest.collectProviderSpecificResources(cloudResourceList, cloudInstances).size());
    }

    @Test
    public void testGetDeletableResourcesWhenEverythingIsFine() {
        List<CloudResource> cloudResourceList = new ArrayList<>();
        cloudResourceList.add(cloudResource("test-1", GCP_RESERVED_IP));
        List<CloudInstance> cloudInstances = new ArrayList<>();
        cloudInstances.add(cloudInstance("test-1"));
        cloudInstances.add(cloudInstance("test-2"));

        assertEquals(1, underTest.getDeletableResources(
                cloudResourceList,
                cloudInstances).size());
    }

    @Test
    public void testCollectResourcesToRemoveWhenEverythingIsFine() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        List<CloudResource> cloudResourceList = new ArrayList<>();
        cloudResourceList.add(cloudResource("test-1", GCP_RESERVED_IP));
        List<CloudInstance> cloudInstances = new ArrayList<>();
        cloudInstances.add(cloudInstance("test-1"));
        cloudInstances.add(cloudInstance("test-2"));

        assertEquals(3, underTest.collectResourcesToRemove(
                authenticatedContext,
                cloudStack,
                cloudResourceList,
                cloudInstances).size());
    }

    @Test
    public void testUpscaleWhenEverythingIsFine() throws QuotaExceededException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        Network network = mock(Network.class);
        CloudContext cloudContext = mock(CloudContext.class);
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        ResourceContextBuilder resourceContextBuilder = mock(ResourceContextBuilder.class);

        List<CloudResource> cloudResourceList = List.of(
                cloudResource("test-5", GCP_ATTACHED_DISKSET)
        );

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudStack.getNetwork()).thenReturn(network);
        when(cloudContext.getPlatform()).thenReturn(platform("GCP"));
        when(cloudContext.getVariant()).thenReturn(GcpConstants.GCP_VARIANT);
        when(contextBuilders.get(any(Platform.class))).thenReturn(resourceContextBuilder);
        when(resourceContextBuilder.contextInit(
                any(CloudContext.class),
                any(AuthenticatedContext.class),
                any(Network.class),
                anyBoolean())).thenReturn(resourceBuilderContext);
        when(networkResourceService.getNetworkResources(any(Variant.class), anyList())).thenReturn(new ArrayList<>());
        when(authenticationResourceService.getAuthenticationResources(any(Variant.class), anyList())).thenReturn(new ArrayList<>());
        doNothing().when(resourceBuilderContext).addNetworkResources(anyCollection());
        when(groupResourceService.getGroupResources(any(Variant.class), anyCollection()))
                .thenReturn(List.of(cloudResource("test-1", GCP_INSTANCE)));
        Group master = group();
        master.getInstances().get(0).putParameter(CloudInstance.FQDN, "fqdn");
        when(cloudStack.getGroups()).thenReturn(List.of(master));
        doNothing().when(resourceBuilderContext).addComputeResources(anyLong(), anyList());
        when(computeResourceService.buildResourcesForUpscale(
                any(ResourceBuilderContext.class),
                any(AuthenticatedContext.class),
                any(CloudStack.class),
                anyCollection(),
                any())
        ).thenReturn(List.of());

        underTest.upscale(authenticatedContext, cloudStack, cloudResourceList, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 0L));

        verify(contextBuilders, times(1)).get(any(Platform.class));
        verify(networkResourceService, times(1)).getNetworkResources(any(Variant.class), anyList());
        verify(groupResourceService, times(1)).getGroupResources(any(Variant.class), anyList());
        verify(resourceBuilderContext, times(1)).addComputeResources(anyLong(), anyList());
        verify(computeResourceService, times(1)).buildResourcesForUpscale(
                any(ResourceBuilderContext.class),
                any(AuthenticatedContext.class),
                any(CloudStack.class),
                anyCollection(),
                any()
        );
    }

    private CloudResource cloudResource(String name, ResourceType resourceType) {
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", false, "fstab", List.of(), 1, "type");
        volumeSetAttributes.setDiscoveryFQDN("fqdn");
        return CloudResource.builder()
                .withType(resourceType)
                .withName(name)
                .withGroup("master")
                .withStatus(CommonStatus.REQUESTED)
                .withParameters(Map.of(CloudResource.ATTRIBUTES, volumeSetAttributes))
                .build();
    }

    private CloudInstance cloudInstance(String name) {
        return new CloudInstance(name, instanceTemplate(), instanceAuthentication(), "subnet-1", "az1");
    }

    private Group group() {
        return Group.builder()
                .withName("master")
                .withType(InstanceGroupType.CORE)
                .withInstances(List.of(cloudInstance()))
                .withSkeleton(cloudInstance())
                .withInstanceAuthentication(instanceAuthentication())
                .build();
    }

    private InstanceTemplate instanceTemplate() {
        return new InstanceTemplate("large", "master", 1L, new ArrayList<>(), InstanceStatus.CREATE_REQUESTED, null, 1L,
                "image", TemporaryStorage.ATTACHED_VOLUMES, 0L);
    }

    private InstanceAuthentication instanceAuthentication() {
        return new InstanceAuthentication("sshkey", "", "cloudbreak");
    }

    private CloudInstance cloudInstance() {
        return new CloudInstance("test-1", instanceTemplate(), instanceAuthentication(), "subnet-1", "az1");
    }
}
