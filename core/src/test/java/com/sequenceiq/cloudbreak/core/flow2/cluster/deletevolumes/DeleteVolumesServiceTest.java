package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesHandlerRequest;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class DeleteVolumesServiceTest {

    private static final String TEST_ENV_CRN = "crn:cdp:environments:us-west-1:accountId:environment:ac5ba74b-c35e-45e9-9f47-123456789876";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private ClusterBootstrapper clusterBootstrapper;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackService stackService;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @InjectMocks
    private DeleteVolumesService underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private CloudResource cloudResource;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private ResourceVolumeConnector resourceVolumeConnector;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterModificationService clusterModificationService;

    @Mock
    private CloudPlatformVariant cloudPlatformVariant;

    @Test
    void detachResourcesSuccess() throws Exception {
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        doReturn(resourceVolumeConnector).when(cloudConnector).volumeConnector();
        List<CloudResource> resources = List.of(cloudResource);
        underTest.detachResources(resources, cloudPlatformVariant, authenticatedContext);
    }

    @Test
    void detachResourcesFailure() throws Exception {
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        doReturn(resourceVolumeConnector).when(cloudConnector).volumeConnector();
        List<CloudResource> resources = List.of(cloudResource);
        doThrow(new Exception("TEST")).when(resourceVolumeConnector).detachVolumes(any(), any());
        Exception exception = assertThrows(Exception.class, () -> underTest.detachResources(resources, cloudPlatformVariant, authenticatedContext));
        assertEquals("TEST", exception.getMessage());
    }

    @Test
    void deleteResourcesSuccess() throws Exception {
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        doReturn(resourceVolumeConnector).when(cloudConnector).volumeConnector();
        List<CloudResource> resources = List.of(cloudResource);
        underTest.deleteResources(resources, cloudPlatformVariant, authenticatedContext);
    }

    @Test
    void deleteResourcesFailure() throws Exception {
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        doReturn(resourceVolumeConnector).when(cloudConnector).volumeConnector();
        List<CloudResource> resources = List.of(cloudResource);
        doThrow(new Exception("TEST")).when(resourceVolumeConnector).deleteVolumes(any(), any());
        Exception exception = assertThrows(Exception.class, () -> underTest.deleteResources(resources, cloudPlatformVariant, authenticatedContext));
        assertEquals("TEST", exception.getMessage());
    }

    @Test
    void updateCbdbResourcesSuccess() throws Exception {
        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup("COMPUTE");
        DeleteVolumesHandlerRequest deleteVolumesRequest = new DeleteVolumesHandlerRequest(List.of(cloudResource), stackDeleteVolumesRequest,
                "MOCK", Set.of());
        doReturn(new HashSet<>()).when(stackDto).getResources();
        underTest.deleteVolumeResources(stackDto, deleteVolumesRequest);
    }

    @Test
    void updateCbdbResourcesFailure() {
        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup("COMPUTE");
        DeleteVolumesHandlerRequest deleteVolumesRequest = new DeleteVolumesHandlerRequest(List.of(cloudResource), stackDeleteVolumesRequest,
                "MOCK", Set.of());
        Resource resource = mock(Resource.class);
        doReturn(Set.of(resource)).when(stackDto).getResources();
        doReturn("COMPUTE").when(resource).getInstanceGroup();
        doReturn(ResourceType.AWS_VOLUMESET).when(resource).getResourceType();
        doThrow(new RuntimeException("TEST")).when(resourceService).deleteAll(anyList());
        Exception exception = assertThrows(Exception.class, () -> underTest.deleteVolumeResources(stackDto, deleteVolumesRequest));
        assertEquals("TEST", exception.getMessage());
        assertEquals(exception.getClass(), RuntimeException.class);
    }

    @Test
    void testStartClouderaManagerService() throws Exception {
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(any(StackDto.class));
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        doReturn(1L).when(stackDto).getId();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);

        underTest.startClouderaManagerService(stackDto, hostTemplateServiceComponents);
        verify(clusterModificationService, times(1)).startClouderaManagerService("yarn", true);
    }

    @Test
    void testStartClouderaManagerServiceException() throws Exception {
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(any(StackDto.class));
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        doReturn(1L).when(stackDto).getId();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);
        doThrow(new Exception("Test")).when(clusterModificationService).startClouderaManagerService("yarn", true);

        Exception exception = assertThrows(Exception.class, () -> underTest.startClouderaManagerService(stackDto, hostTemplateServiceComponents));
        assertEquals("Unable to start CM services for service yarn, in stack 1: Test", exception.getMessage());
    }

    @Test
    void testStopClouderaManagerService() throws Exception {
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(any(StackDto.class));
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        doReturn(1L).when(stackDto).getId();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);

        underTest.stopClouderaManagerService(stackDto, hostTemplateServiceComponents);
        verify(clusterModificationService, times(1)).stopClouderaManagerService("yarn", true);
    }

    @Test
    void testStopClouderaManagerServiceException() throws Exception {
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(any(StackDto.class));
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        doReturn(1L).when(stackDto).getId();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);
        doThrow(new Exception("Test")).when(clusterModificationService).stopClouderaManagerService("yarn", true);

        Exception exception = assertThrows(Exception.class, () -> underTest.stopClouderaManagerService(stackDto, hostTemplateServiceComponents));
        assertEquals("Unable to stop CM services for service yarn, in stack 1: Test", exception.getMessage());
    }

    @Test
    void testUnmountBlockStorageDisks() throws Exception {
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(any(StackDto.class));
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        Stack stack = mock(Stack.class);
        Cluster cluster = mock(Cluster.class);
        doReturn(1L).when(cluster).getId();
        doReturn(cluster).when(stack).getCluster();
        doReturn("bp").when(stack).getBlueprintJsonText();
        CmTemplateProcessor processor = mock(CmTemplateProcessor.class);
        doReturn(processor).when(cmTemplateProcessorFactory).get("bp");
        doReturn(1L).when(stack).getId();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);
        Map<String, Set<ServiceComponent>> hostTemplateComponents = Map.of("test", hostTemplateServiceComponents);
        doReturn(hostTemplateComponents).when(processor).getServiceComponentsByHostGroup();

        StackDto stackDto = mock(StackDto.class);
        doReturn(stackDto).when(stackDtoService).getById(1L);
        Node node = mock(Node.class);
        doReturn("test").when(node).getHostGroup();
        Set<Node> nodes = Set.of(node);
        doReturn(nodes).when(stackUtil).collectNodes(stack);
        doReturn(nodes).when(stackUtil).collectNodesWithDiskData(stack);
        underTest.unmountBlockStorageDisks(stack, "test");
        verify(clusterModificationService, times(1)).stopClouderaManagerService("yarn", true);
        verify(clusterBootstrapper, times(1)).reBootstrapMachines(1L);
        verify(hostOrchestrator, times(1)).unmountBlockStorageDisks(anyList(), eq(nodes), eq(nodes), any());
    }

    @Test
    void testUnmountBlockStorageDisksException() throws Exception {
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(any(StackDto.class));
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        Stack stack = mock(Stack.class);
        Cluster cluster = mock(Cluster.class);
        doReturn(1L).when(cluster).getId();
        doReturn(cluster).when(stack).getCluster();
        doReturn("bp").when(stack).getBlueprintJsonText();
        CmTemplateProcessor processor = mock(CmTemplateProcessor.class);
        doReturn(processor).when(cmTemplateProcessorFactory).get("bp");
        doReturn(1L).when(stack).getId();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);
        Map<String, Set<ServiceComponent>> hostTemplateComponents = Map.of("test", hostTemplateServiceComponents);
        doReturn(hostTemplateComponents).when(processor).getServiceComponentsByHostGroup();

        StackDto stackDto = mock(StackDto.class);
        doReturn(stackDto).when(stackDtoService).getById(1L);
        Node node = mock(Node.class);
        doReturn("test").when(node).getHostGroup();
        Set<Node> nodes = Set.of(node);
        doReturn(nodes).when(stackUtil).collectNodes(stack);
        doReturn(nodes).when(stackUtil).collectNodesWithDiskData(stack);
        doThrow(new CloudbreakOrchestratorFailedException("Test")).when(hostOrchestrator).unmountBlockStorageDisks(anyList(), eq(nodes), eq(nodes), any());

        Exception exception = assertThrows(Exception.class, () -> underTest.unmountBlockStorageDisks(stack, "test"));
        assertEquals("Test", exception.getMessage());
        assertEquals(exception.getClass(), CloudbreakOrchestratorFailedException.class);
    }

    @Test
    void testUpdateScriptsAndRebootInstances() throws Exception {
        StackDto stackDto = mock(StackDto.class);
        doReturn(stackDto).when(stackDtoService).getById(1L);
        Resource resource = mock(Resource.class);
        Stack stack = getMockStack(resource);

        InstanceGroupDto instanceGroupDto = mock(InstanceGroupDto.class);
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        doReturn("test").when(instanceGroup).getGroupName();
        doReturn(instanceGroup).when(instanceGroupDto).getInstanceGroup();
        InstanceMetadataView instanceMetadataView = mock(InstanceMetadataView.class);
        List<InstanceMetadataView> instanceMetadataViews = List.of(instanceMetadataView);
        doReturn(instanceMetadataViews).when(instanceGroupDto).getNotDeletedInstanceMetaData();
        doReturn(List.of(instanceGroupDto)).when(stack).getInstanceGroupDtos();
        CloudInstance cloudInstance = mock(CloudInstance.class);
        doReturn(List.of(cloudInstance)).when(instanceMetaDataToCloudInstanceConverter).convert(instanceMetadataViews, stack);
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        CloudCredential cloudCredential = mock(CloudCredential.class);
        doReturn(cloudCredential).when(stackUtil).getCloudCredential(any());

        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        doReturn(authenticator).when(cloudConnector).authentication();
        doReturn(ac).when(authenticator).authenticate(any(), any());
        InstanceConnector ic = mock(InstanceConnector.class);
        doReturn(ic).when(cloudConnector).instances();

        underTest.updateScriptsAndRebootInstances(1L, "test");
        verify(clusterHostServiceRunner, times(1)).updateClusterConfigs(stackDto, true);
        verify(ic, times(1)).reboot(ac, null, List.of(cloudInstance));
    }

    @Test
    void testUpdateScriptsAndRebootInstancesException() throws Exception {
        StackDto stackDto = mock(StackDto.class);
        doReturn(stackDto).when(stackDtoService).getById(1L);

        doThrow(new CloudbreakServiceException("Test")).when(clusterHostServiceRunner).updateClusterConfigs(stackDto, true);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.updateScriptsAndRebootInstances(1L, "test"));
        assertEquals("Test", exception.getMessage());
    }

    private Stack getMockStack(Resource resource) {
        Stack stack = mock(Stack.class);
        doReturn("AWS").when(stack).getCloudPlatform();
        doReturn("AWS").when(stack).getPlatformVariant();
        doReturn(stack).when(stackService).getByIdWithLists(1L);
        doReturn(ResourceType.AWS_VOLUMESET).when(stack).getDiskResourceType();

        doReturn(List.of(resource)).when(resourceService).findAllByStackIdAndResourceTypeIn(1L, List.of(ResourceType.AWS_VOLUMESET));
        doReturn("test-instance-id").when(resource).getInstanceId();
        doReturn("test-stack").when(stack).getName();
        Workspace workspace = mock(Workspace.class);
        doReturn(1L).when(workspace).getId();
        doReturn(workspace).when(stack).getWorkspace();
        doReturn(TEST_ENV_CRN).when(stack).getResourceCrn();
        doReturn("us-west-1").when(stack).getRegion();
        doReturn("us-west-1a").when(stack).getAvailabilityZone();

        return stack;
    }
}
