package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.common.api.type.ResourceType.GCP_ATTACHED_DISKSET;
import static com.sequenceiq.common.api.type.ResourceType.GCP_INSTANCE;
import static com.sequenceiq.common.api.type.ResourceType.GCP_RESERVED_IP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
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
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.group.GroupResourceService;
import com.sequenceiq.cloudbreak.cloud.template.init.ContextBuilders;
import com.sequenceiq.cloudbreak.cloud.template.network.NetworkResourceService;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class GcpResourceConnectorTest {

    @InjectMocks
    private GcpResourceConnector underTest;

    @Mock
    private NetworkResourceService networkResourceService;

    @Mock
    private GroupResourceService groupResourceService;

    @Mock
    private ComputeResourceService computeResourceService;

    @Mock
    private ContextBuilders contextBuilders;

    @Test
    public void testGetDBStackTemplateWhenEverythingIsFine() throws TemplatingNotSupportedException {
        Assert.assertTrue(underTest.getDBStackTemplate().equals(""));
    }

    @Test
    public void testGetStackTemplateWhenEverythingIsFine() {
        assertThrows(TemplatingNotSupportedException.class, () -> underTest.getStackTemplate());
    }

    @Test
    public void testGetTlsInfoWhenEverythingIsfine() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        Assert.assertEquals(false, underTest.getTlsInfo(authenticatedContext,  cloudStack).usePrivateIpToTls());
    }

    @Test
    public void testLaunchLoadBalancersWhenEverythingIsFine() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);

        assertThrows(UnsupportedOperationException.class,
                () -> underTest.launchLoadBalancers(authenticatedContext, cloudStack, persistenceNotifier));
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
    public void testUpscaleWhenEverythingIsFine() {
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
        when(contextBuilders.get(any(Platform.class))).thenReturn(resourceContextBuilder);
        when(resourceContextBuilder.contextInit(
                any(CloudContext.class),
                any(AuthenticatedContext.class),
                any(Network.class),
                anyList(),
                anyBoolean())).thenReturn(resourceBuilderContext);
        when(networkResourceService.getNetworkResources(any(Platform.class), anyList())).thenReturn(new ArrayList<>());
        doNothing().when(resourceBuilderContext).addNetworkResources(anyCollection());
        when(groupResourceService.getGroupResources(any(Platform.class), anyCollection()))
                .thenReturn(List.of(cloudResource("test-1", GCP_INSTANCE)));
        when(cloudStack.getGroups()).thenReturn(List.of(group("master")));
        doNothing().when(resourceBuilderContext).addComputeResources(anyLong(), anyList());
        when(computeResourceService.buildResourcesForUpscale(
                any(ResourceBuilderContext.class),
                any(AuthenticatedContext.class),
                any(CloudStack.class),
                anyCollection())
        ).thenReturn(List.of());

        underTest.upscale(authenticatedContext, cloudStack, cloudResourceList);

        verify(contextBuilders, times(1)).get(any(Platform.class));
        verify(networkResourceService, times(1)).getNetworkResources(any(Platform.class), anyList());
        verify(groupResourceService, times(1)).getGroupResources(any(Platform.class), anyList());
        verify(resourceBuilderContext, times(1)).addComputeResources(anyLong(), anyList());
        verify(computeResourceService, times(1)).buildResourcesForUpscale(
                any(ResourceBuilderContext.class),
                any(AuthenticatedContext.class),
                any(CloudStack.class),
                anyCollection()
        );
    }

    private CloudResource cloudResource(String name, ResourceType resourceType) {
        return CloudResource.builder()
                .type(resourceType)
                .name(name)
                .group("master")
                .status(CommonStatus.REQUESTED)
                .params(Map.of())
                .build();
    }

    private CloudInstance cloudInstance(String name) {
        return new CloudInstance(name, instanceTemplate(), instanceAuthentication());
    }

    private Group group(String name) {
        return new Group(
                name,
                InstanceGroupType.CORE,
                List.of(cloudInstance()),
                new Security(List.of(), List.of()),
                cloudInstance(),
                instanceAuthentication(),
                "loginUserName",
                "publicKey",
                50,
                Optional.empty());
    }

    private InstanceTemplate instanceTemplate() {
        return new InstanceTemplate("large", "master", 1L, new ArrayList<>(), InstanceStatus.CREATE_REQUESTED, null, 1L,
                "image", TemporaryStorage.ATTACHED_VOLUMES);
    }

    private InstanceAuthentication instanceAuthentication() {
        return new InstanceAuthentication("sshkey", "", "cloudbreak");
    }

    private CloudInstance cloudInstance() {
        return new CloudInstance("test-1", instanceTemplate(), instanceAuthentication());
    }
}