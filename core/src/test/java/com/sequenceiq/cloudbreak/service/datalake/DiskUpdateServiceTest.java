package com.sequenceiq.cloudbreak.service.datalake;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.DISK_RESIZE_TRIGGER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterCache;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeRequest;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class DiskUpdateServiceTest {

    private static final long STACK_ID = 1L;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CloudParameterCache cloudParameterCache;

    @Mock
    private StackService stackService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ReactorNotifier reactorNotifier;

    @Mock
    private TemplateService templateService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private DiskUpdateService underTest;

    @Test
    void testIsDiskTypeChangeSupported() {
        String platform = "AWS";
        doReturn(true).when(cloudParameterCache).isDiskTypeChangeSupported(platform);
        boolean diskTypeSupported = underTest.isDiskTypeChangeSupported(platform);
        assertTrue(diskTypeSupported);
        verify(cloudParameterCache, times(1)).isDiskTypeChangeSupported(platform);
    }

    @Test
    void testIsDiskTypeChangeNotSupported() {
        String platform = "AWS";
        doReturn(false).when(cloudParameterCache).isDiskTypeChangeSupported(platform);
        boolean diskTypeSupported = underTest.isDiskTypeChangeSupported(platform);
        assertFalse(diskTypeSupported);
        verify(cloudParameterCache, times(1)).isDiskTypeChangeSupported(platform);
    }

    @Test
    void testStopCMServices() throws Exception {
        StackDto stackDto = mock(StackDto.class);
        ClusterApi clusterApi = mock(ClusterApi.class);
        doReturn(stackDto).when(stackDtoService).getById(STACK_ID);
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(stackDto);
        ClusterModificationService clusterModificationService = mock(ClusterModificationService.class);
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        underTest.stopCMServices(STACK_ID);
    }

    @Test
    void testStopCMServicesException() throws Exception {
        StackDto stackDto = mock(StackDto.class);
        ClusterApi clusterApi = mock(ClusterApi.class);
        doReturn(stackDto).when(stackDtoService).getById(STACK_ID);
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(stackDto);
        ClusterModificationService clusterModificationService = mock(ClusterModificationService.class);
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        CloudbreakException exception = new CloudbreakException("TEST_EXCEPTION");
        doThrow(exception).when(clusterModificationService).stopCluster(true);
        Exception returnException = assertThrows(Exception.class, () -> underTest.stopCMServices(STACK_ID));
        assertEquals("TEST_EXCEPTION", returnException.getMessage());
    }

    @Test
    void testStartCMServices() throws Exception {
        StackDto stackDto = mock(StackDto.class);
        ClusterApi clusterApi = mock(ClusterApi.class);
        doReturn(stackDto).when(stackDtoService).getById(STACK_ID);
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(stackDto);
        ClusterModificationService clusterModificationService = mock(ClusterModificationService.class);
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        underTest.startCMServices(STACK_ID);
    }

    @Test
    void testStartCMServicesException() throws Exception {
        StackDto stackDto = mock(StackDto.class);
        ClusterApi clusterApi = mock(ClusterApi.class);
        doReturn(stackDto).when(stackDtoService).getById(STACK_ID);
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(stackDto);
        ClusterModificationService clusterModificationService = mock(ClusterModificationService.class);
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        CloudbreakException exception = new CloudbreakException("TEST_EXCEPTION");
        doThrow(exception).when(clusterModificationService).startCluster();
        Exception returnException = assertThrows(Exception.class, () -> underTest.startCMServices(STACK_ID));
        assertEquals("TEST_EXCEPTION", returnException.getMessage());
    }

    @Test
    void testResizeDisks() throws Exception {
        Stack stack = mock(Stack.class);
        doReturn(stack).when(stackService).getByIdWithListsInTransaction(STACK_ID);
        DiskUpdateRequest diskUpdateRequest = mock(DiskUpdateRequest.class);
        List<Volume> volumeListToUpdate = List.of(mock(Volume.class));
        underTest.resizeDisks(STACK_ID, "test", "gp2", 100, volumeListToUpdate);

        ArgumentCaptor<DiskResizeRequest> captor = ArgumentCaptor.forClass(DiskResizeRequest.class);
        verify(reactorNotifier).notify(any(), any(), captor.capture());
        assertEquals("test", captor.getValue().getInstanceGroup());
        assertEquals(DISK_RESIZE_TRIGGER_EVENT.selector(), captor.getValue().getSelector());
        assertEquals(STACK_ID, captor.getValue().getResourceId());
        assertEquals(volumeListToUpdate, captor.getValue().getVolumesToUpdate());
    }

    @Test
    void testUpdateDiskTypeAndSize() throws Exception {
        String instanceGroup = "master";
        Long stackId = 1L;
        CloudConnector cloudConnector = mock(CloudConnector.class);
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        AwsResourceVolumeConnector awsResourceVolumeConnector = mock(AwsResourceVolumeConnector.class);
        doReturn(awsResourceVolumeConnector).when(cloudConnector).volumeConnector();
        DiskTypes diskTypes = mock(DiskTypes.class);
        when(diskTypes.diskMapping()).thenReturn(
                Map.of(
                        "ephemeral", VolumeParameterType.EPHEMERAL,
                        "st1", VolumeParameterType.ST1
                )
        );
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        when(platformParameters.diskTypes()).thenReturn(diskTypes);
        when(cloudConnector.parameters()).thenReturn(platformParameters);
        StackDto stack = mock(StackDto.class);
        doReturn("AWS").when(stack).getPlatformVariant();
        doReturn("AWS").when(stack).getCloudPlatform();
        Workspace workspace = mock(Workspace.class);
        doReturn(workspace).when(stack).getWorkspace();
        doReturn(stackId).when(workspace).getId();
        doReturn("crn:cdp:iam:us-west-1:someworkspace:user:someuser").when(stack).getResourceCrn();
        Authenticator authenticator  = mock(Authenticator.class);
        doReturn(authenticator).when(cloudConnector).authentication();
        doReturn(stack).when(stackDtoService).getById(stackId);

        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        Template template = mock(Template.class);
        doReturn(template).when(instanceGroupView).getTemplate();
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(VolumeParameterType.ST1.name().toLowerCase());
        doReturn(Set.of(volumeTemplate)).when(template).getVolumeTemplates();
        doReturn(Optional.of(instanceGroupView)).when(instanceGroupService).findInstanceGroupViewByStackIdAndGroupName(stackId, instanceGroup);
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setSize(200);
        diskUpdateRequest.setVolumeType(VolumeParameterType.ST1.name().toLowerCase());
        diskUpdateRequest.setGroup("master");
        Volume volume = mock(Volume.class);
        doReturn("vol-1").when(volume).getId();
        underTest.updateDiskTypeAndSize("master", "st1", 200, List.of(volume), stackId);

        verify(templateService).savePure(template);
        verify(awsResourceVolumeConnector).updateDiskVolumes(any(), eq(List.of("vol-1")), eq("st1"), eq(200));
        verify(resourceService).saveAll(any());
        VolumeTemplate resultVolumeTemplate = template.getVolumeTemplates().stream().findFirst().get();
        assertEquals(200, resultVolumeTemplate.getVolumeSize());
        assertEquals("st1", resultVolumeTemplate.getVolumeType());
    }

    @Test
    void testUpdateDiskTypeAndSizeResourceUpdated() throws Exception {
        AwsResourceVolumeConnector awsResourceVolumeConnector = mock(AwsResourceVolumeConnector.class);
        Template template = mock(Template.class);
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setSize(200);
        diskUpdateRequest.setVolumeType("st1");
        diskUpdateRequest.setGroup("master");
        Volume volume1 = mock(Volume.class);
        doReturn("vol-1").when(volume1).getId();
        setUpForDiskResize(awsResourceVolumeConnector, template);
        underTest.updateDiskTypeAndSize("master", "st1", 200, List.of(volume1), 1L);

        verify(templateService).savePure(template);
        verify(awsResourceVolumeConnector).updateDiskVolumes(any(), eq(List.of("vol-1")), eq("st1"), eq(200));
        ArgumentCaptor<List<Resource>> saveResourcesCaptor = ArgumentCaptor.forClass(List.class);
        verify(resourceService).saveAll(saveResourcesCaptor.capture());
        List<VolumeSetAttributes.Volume> volResult = saveResourcesCaptor.getValue().get(0).getAttributes().get(VolumeSetAttributes.class)
                .getVolumes();
        VolumeSetAttributes.Volume updatedVolume = volResult.stream().filter(vol -> vol.getId().equals("vol-1")).toList().get(0);
        VolumeSetAttributes.Volume notUpdatedVolume = volResult.stream().filter(vol -> vol.getId().equals("vol-2")).toList().get(0);
        assertEquals(200, updatedVolume.getSize());
        assertEquals(100, notUpdatedVolume.getSize());
        VolumeTemplate resultVolumeTemplate = template.getVolumeTemplates().stream().findFirst().get();
        assertEquals(200, resultVolumeTemplate.getVolumeSize());
        assertEquals("st1", resultVolumeTemplate.getVolumeType());
    }

    @Test
    void testUpdateDiskTypeAndSizeEntitlementNotAssignedForAzure() throws Exception {
        Long stackId = 1L;
        String tenant = "default";
        StackDto stack = mock(StackDto.class);
        doReturn("AZURE").when(stack).getCloudPlatform();
        doReturn(String.format("crn:cdp:iam:us-west-1:%s:user:someuser", tenant)).when(stack).getResourceCrn();
        doReturn(stack).when(stackDtoService).getById(stackId);
        doReturn(false).when(entitlementService).azureResizeDiskEnabled(tenant);

        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setSize(200);
        diskUpdateRequest.setGroup("master");
        diskUpdateRequest.setVolumeType("test");

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.updateDiskTypeAndSize("master", "test", 200, List.of(), stackId));
        assertEquals("Resizing Disk for Azure is not enabled for this account", badRequestException.getMessage());
    }

    @Test
    void testUpdateDiskTypeAndSizeVolumeTypeSpecifiedForAzure() throws Exception {
        Long stackId = 1L;
        String tenant = "default";
        StackDto stack = mock(StackDto.class);
        doReturn("AZURE").when(stack).getCloudPlatform();
        doReturn(String.format("crn:cdp:iam:us-west-1:%s:user:someuser", tenant)).when(stack).getResourceCrn();
        doReturn(stack).when(stackDtoService).getById(stackId);
        doReturn(true).when(entitlementService).azureResizeDiskEnabled(tenant);

        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setSize(200);
        diskUpdateRequest.setGroup("master");
        diskUpdateRequest.setVolumeType("test");

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.updateDiskTypeAndSize("master", "test", 200, List.of(), stackId));
        assertEquals("Changing Volume Type is not supported for Azure", badRequestException.getMessage());
    }

    private void setUpForDiskResize(AwsResourceVolumeConnector awsResourceVolumeConnector, Template template) {
        String instanceGroup = "master";
        Long stackId = 1L;
        CloudConnector cloudConnector = mock(CloudConnector.class);
        DiskTypes diskTypes = mock(DiskTypes.class);
        when(diskTypes.diskMapping()).thenReturn(
                Map.of(
                        "ephemeral", VolumeParameterType.EPHEMERAL,
                        "st1", VolumeParameterType.ST1
                )
        );
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        when(platformParameters.diskTypes()).thenReturn(diskTypes);
        when(cloudConnector.parameters()).thenReturn(platformParameters);
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        doReturn(awsResourceVolumeConnector).when(cloudConnector).volumeConnector();
        StackDto stack = mock(StackDto.class);
        doReturn("AWS").when(stack).getPlatformVariant();
        doReturn("AWS").when(stack).getCloudPlatform();
        Workspace workspace = mock(Workspace.class);
        doReturn(workspace).when(stack).getWorkspace();
        doReturn(stackId).when(workspace).getId();
        doReturn("crn:cdp:iam:us-west-1:someworkspace:user:someuser").when(stack).getResourceCrn();
        Authenticator authenticator  = mock(Authenticator.class);
        doReturn(authenticator).when(cloudConnector).authentication();
        Resource resource = new Resource();
        resource.setInstanceGroup("master");
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("vol-1", "", 100, "st1", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume volume2 = new VolumeSetAttributes.Volume("vol-2", "", 100, "st1", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", false, "", List.of(volume, volume2), 100, "");
        resource.setAttributes(Json.silent(volumeSetAttributes));
        doCallRealMethod().when(resourceAttributeUtil).getTypedAttributes(eq(resource), eq(VolumeSetAttributes.class));
        doCallRealMethod().when(resourceAttributeUtil).setTypedAttributes(any(), any());
        doReturn(stack).when(stackDtoService).getById(stackId);
        doReturn(List.of(resource)).when(stack).getDiskResources();

        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);

        doReturn(template).when(instanceGroupView).getTemplate();
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeCount(1);
        volumeTemplate.setVolumeType("st1");
        doReturn(Set.of(volumeTemplate)).when(template).getVolumeTemplates();
        doReturn(Optional.of(instanceGroupView))
                .when(instanceGroupService)
                .findInstanceGroupViewByStackIdAndGroupName(stackId, instanceGroup);
    }

    @Test
    void testResizeDisksAndUpdateFstab() throws CloudbreakOrchestratorFailedException {
        Stack stack = mock(Stack.class);
        doReturn(ResourceType.AWS_VOLUMESET).when(stack).getDiskResourceType();
        doReturn(STACK_ID).when(stack).getId();
        Resource resource = mock(Resource.class);
        doReturn("test-instance-1").when(resource).getInstanceId();
        List<Resource> resourceList = List.of(resource);
        doReturn(resourceList).when(resourceService).findAllByStackIdAndInstanceGroupAndResourceTypeIn(eq(STACK_ID), eq("compute"), anyList());
        doReturn(resourceList).when(stack).getDiskResources();
        Node node = mock(Node.class);
        doReturn("compute").when(node).getHostGroup();
        Set<Node> allNodes = Set.of(node);
        doReturn(allNodes).when(stackUtil).collectNodes(stack);
        doReturn(allNodes).when(stackUtil).collectNodesWithDiskData(stack);
        Cluster cluster = mock(Cluster.class);
        doReturn(cluster).when(stack).getCluster();
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        doReturn("test-instance-fqdn").when(instanceMetaData).getDiscoveryFQDN();
        doReturn("test-instance-1").when(instanceMetaData).getInstanceId();
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);
        doReturn(instanceMetaDataList).when(stack).getInstanceMetaDataAsList();
        Map<String, Map<String, String>> fstabInformation = Map.of("test-instance-fqdn", Map.of("uuids", "test-uuid", "fstab", "test-fstab"));
        doReturn(fstabInformation).when(hostOrchestrator).resizeDisksOnNodes(anyList(), anySet(), anySet(), any());
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);
        doReturn("test-instance-fqdn").when(volumeSetAttributes).getDiscoveryFQDN();
        doReturn(Optional.of(volumeSetAttributes)).when(resourceAttributeUtil).getTypedAttributes(any(), eq(VolumeSetAttributes.class));

        underTest.resizeDisksAndUpdateFstab(stack, "compute");
        verify(resourceService).findAllByStackIdAndInstanceGroupAndResourceTypeIn(STACK_ID, "compute", List.of(ResourceType.AWS_VOLUMESET));
        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        verify(hostOrchestrator).resizeDisksOnNodes(anyList(), anySet(), anySet(), any());
        verify(resourceService).saveAll(resourceList);
    }

    @Test
    void testResizeDisksAndUpdateFstabException() throws CloudbreakOrchestratorFailedException {
        Stack stack = mock(Stack.class);
        doReturn(ResourceType.AWS_VOLUMESET).when(stack).getDiskResourceType();
        doReturn(STACK_ID).when(stack).getId();
        Resource resource = mock(Resource.class);
        doReturn("test-instance-1").when(resource).getInstanceId();
        List<Resource> resourceList = List.of(resource);
        doReturn(resourceList).when(resourceService).findAllByStackIdAndInstanceGroupAndResourceTypeIn(eq(STACK_ID), eq("compute"), anyList());
        Node node = mock(Node.class);
        doReturn("compute").when(node).getHostGroup();
        Set<Node> allNodes = Set.of(node);
        doReturn(allNodes).when(stackUtil).collectNodes(stack);
        doReturn(allNodes).when(stackUtil).collectNodesWithDiskData(stack);
        Cluster cluster = mock(Cluster.class);
        doReturn(cluster).when(stack).getCluster();

        doThrow(new CloudbreakOrchestratorFailedException("Test Exception")).when(hostOrchestrator).resizeDisksOnNodes(anyList(), anySet(), anySet(), any());

        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.resizeDisksAndUpdateFstab(stack, "compute"));
        verify(resourceService).findAllByStackIdAndInstanceGroupAndResourceTypeIn(STACK_ID, "compute", List.of(ResourceType.AWS_VOLUMESET));
        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        verify(hostOrchestrator).resizeDisksOnNodes(anyList(), anySet(), anySet(), any());
        assertEquals("Test Exception", exception.getMessage());
    }
}
