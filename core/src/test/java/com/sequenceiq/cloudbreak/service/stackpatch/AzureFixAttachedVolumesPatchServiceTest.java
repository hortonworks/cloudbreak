package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.FileReaderUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudConnectResources;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.salt.PartialSaltStateUpdateService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.util.CloudConnectorHelper;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AzureFixAttachedVolumesPatchServiceTest {
    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private SaltOrchestrator saltOrchestrator;

    @Mock
    private CloudConnectorHelper cloudConnectorHelper;

    @Mock
    private ResourceVolumeConnector volumeConnector;

    @Mock
    private ResourceToCloudResourceConverter resourceConverter;

    @Mock
    private PartialSaltStateUpdateService partialSaltStateUpdateService;

    @Mock
    private SaltStateParamsService saltStateParamsService;

    @Mock
    private ResourceNotifier resourceNotifier;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private AzureFixAttachedVolumesPatchService underTest;

    @Test
    void testIsAffectedWhenNoAzure() {
        Stack stack = new Stack();
        stack.setCloudPlatform(CloudPlatform.GCP.name());

        boolean affected = underTest.isAffected(stack);

        assertFalse(affected);
    }

    @Test
    void testIsAffectedWhenAzureWithNoResources() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setPlatformVariant(CloudConstants.AZURE);
        stack.setCloudPlatform(CloudPlatform.AZURE.name());
        when(resourceService.findAllByResourceStatusAndResourceTypeAndStackId(any(), any(), any())).thenReturn(List.of());

        boolean affected = underTest.isAffected(stack);

        assertFalse(affected);
    }

    @Test
    void testIsAffectedWhenAzureWithNewResources() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setPlatformVariant(CloudConstants.AZURE);
        stack.setCloudPlatform(CloudPlatform.AZURE.name());
        VolumeSetAttributes volumeSetAttributes = createVolumeSetAttributes(null,
                List.of(new VolumeSetAttributes.Volume("id", "/dev/disk/azure/scsi1/lun1", 10, "type", CloudVolumeUsageType.GENERAL)));
        Resource volumeSetResource = createVolumeSetResource("instance1", volumeSetAttributes).getKey();
        when(resourceService.findAllByResourceStatusAndResourceTypeAndStackId(any(), any(), any())).thenReturn(List.of(volumeSetResource));
        when(resourceAttributeUtil.getTypedAttributes(volumeSetResource, VolumeSetAttributes.class)).thenReturn(Optional.of(volumeSetAttributes));

        boolean affected = underTest.isAffected(stack);

        assertFalse(affected);
    }

    @Test
    void testIsAffectedWhenAzureWithDeprecatedResources() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setPlatformVariant(CloudConstants.AZURE);
        stack.setCloudPlatform(CloudPlatform.AZURE.name());
        VolumeSetAttributes volumeSetAttributes =
                createVolumeSetAttributes(null, List.of(new VolumeSetAttributes.Volume("id", "/dev/sdc", 10, "type", CloudVolumeUsageType.GENERAL)));
        Resource volumeSetResource = createVolumeSetResource("instance1", volumeSetAttributes).getKey();
        when(resourceService.findAllByResourceStatusAndResourceTypeAndStackId(any(), any(), any())).thenReturn(List.of(volumeSetResource));
        when(resourceAttributeUtil.getTypedAttributes(volumeSetResource, VolumeSetAttributes.class)).thenReturn(Optional.of(volumeSetAttributes));

        boolean affected = underTest.isAffected(stack);

        assertTrue(affected);
    }

    @Test
    void testDoApplyStackNotAvailable() throws Exception {
        Stack stack = new Stack();
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.STOPPED);
        stack.setStackStatus(stackStatus);

        boolean patchApplied = underTest.doApply(stack);

        assertFalse(patchApplied);
        verify(resourceNotifier, never()).notifyUpdates(anyList(), any());
        verify(saltOrchestrator, never()).updateMountDiskPillar(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void testDoApplyNotAllTheNodesAreReachable() throws Exception {
        Stack stack = new Stack();
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);
        GatewayConfig primaryGatewayConfig = GatewayConfig.builder().build();
        List<GatewayConfig> gatewayConfigs = List.of(primaryGatewayConfig);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(primaryGatewayConfig);

        when(stackUtil.collectNodes(stack)).thenReturn(Set.of(mock(Node.class), mock(Node.class)));
        NodeReachabilityResult nodeReachabilityResult = new NodeReachabilityResult(Set.of(), Set.of());
        when(saltOrchestrator.getResponsiveNodes(any(), any(), eq(Boolean.TRUE))).thenReturn(nodeReachabilityResult);

        boolean patchApplied = underTest.doApply(stack);

        assertFalse(patchApplied);
        verify(resourceNotifier, never()).notifyUpdates(anyList(), any());
        verify(saltOrchestrator, never()).updateMountDiskPillar(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void testDoApplyMissingInstanceFromVolumeDeviceMapping() throws Exception {
        TestParameters parameters = setupDefaultsForFailures("fstab");
        when(volumeConnector.getVolumeDeviceMappingByInstance(parameters.cloudConnectResources.getAuthenticatedContext(),
                parameters.cloudConnectResources.getCloudStack())).thenReturn(Map.of("instance2", Map.of()));

        boolean patchApplied = underTest.doApply(parameters.stack);

        assertFalse(patchApplied);
        verify(resourceNotifier, never()).notifyUpdates(anyList(), any());
        verify(saltOrchestrator, never()).updateMountDiskPillar(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void testDoApplyMissingNodesFromSaltResponse() throws Exception {
        TestParameters parameters = setupDefaultsForFailures("fstab");
        when(volumeConnector.getVolumeDeviceMappingByInstance(parameters.cloudConnectResources.getAuthenticatedContext(),
                parameters.cloudConnectResources.getCloudStack())).thenReturn(Map.of("instance1", Map.of(), "instance2", Map.of(), "instance3", Map.of()));
        OrchestratorStateParams orchestratorStateParams = new OrchestratorStateParams();
        when(saltStateParamsService.createStateParams(parameters.stack, "disks/patch/get-uuids", 3, 3, parameters.primaryGatewayConfig, parameters.nodes))
                .thenReturn(orchestratorStateParams);
        when(saltOrchestrator.applyOrchestratorState(orchestratorStateParams)).thenReturn(createSaltResponse("missingnode-saltresponse.json"));

        boolean patchApplied = underTest.doApply(parameters.stack);

        assertFalse(patchApplied);
        verify(resourceNotifier, never()).notifyUpdates(anyList(), any());
        verify(saltOrchestrator, never()).updateMountDiskPillar(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void testDoApplyValidSaltResponseMissingVolumeIdDeviceNameMapping() throws Exception {
        TestParameters parameters = setupDefaultsForFailures("fstab");
        when(volumeConnector.getVolumeDeviceMappingByInstance(parameters.cloudConnectResources.getAuthenticatedContext(),
                parameters.cloudConnectResources.getCloudStack())).thenReturn(Map.of("instance1", Map.of(), "instance2", Map.of(), "instance3", Map.of()));
        OrchestratorStateParams orchestratorStateParams = new OrchestratorStateParams();
        when(saltStateParamsService.createStateParams(parameters.stack, "disks/patch/get-uuids", 3, 3, parameters.primaryGatewayConfig, parameters.nodes))
                .thenReturn(orchestratorStateParams);
        when(saltOrchestrator.applyOrchestratorState(orchestratorStateParams)).thenReturn(createSaltResponse("valid-saltresponse.json"));

        ExistingStackPatchApplyException exception = assertThrows(ExistingStackPatchApplyException.class, () -> underTest.doApply(parameters.stack));

        assertTrue(exception.getMessage().contains("Missing device name for"));
        verify(resourceNotifier, never()).notifyUpdates(anyList(), any());
        verify(saltOrchestrator, never()).updateMountDiskPillar(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void testDoApplyValidSaltResponseMissingMountLineInFstab() throws Exception {
        TestParameters parameters = setupDefaultsForFailures("fstab");
        when(volumeConnector.getVolumeDeviceMappingByInstance(parameters.cloudConnectResources.getAuthenticatedContext(),
                parameters.cloudConnectResources.getCloudStack())).thenReturn(Map.of("instance1",
                Map.of("id", "lun1"), "instance2", Map.of(), "instance3", Map.of()));
        OrchestratorStateParams orchestratorStateParams = new OrchestratorStateParams();
        when(saltStateParamsService.createStateParams(parameters.stack, "disks/patch/get-uuids", 3, 3, parameters.primaryGatewayConfig, parameters.nodes))
                .thenReturn(orchestratorStateParams);
        when(saltOrchestrator.applyOrchestratorState(orchestratorStateParams)).thenReturn(createSaltResponse("valid-saltresponse.json"));

        ExistingStackPatchApplyException exception = assertThrows(ExistingStackPatchApplyException.class, () -> underTest.doApply(parameters.stack));

        assertTrue(exception.getMessage().contains("Missing mountPath for"));
        verify(resourceNotifier, never()).notifyUpdates(anyList(), any());
        verify(saltOrchestrator, never()).updateMountDiskPillar(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void testDoApplyDuplicatesInFstabOnDifferentPathes() throws Exception {
        TestParameters parameters = setupDefaultsForFailures(createFstab("fstab-duplicate-uuids-different-path.txt"));
        when(volumeConnector.getVolumeDeviceMappingByInstance(parameters.cloudConnectResources.getAuthenticatedContext(),
                parameters.cloudConnectResources.getCloudStack())).thenReturn(Map.of("instance1", Map.of(), "instance2", Map.of(), "instance3", Map.of()));
        OrchestratorStateParams orchestratorStateParams = new OrchestratorStateParams();
        when(saltStateParamsService.createStateParams(parameters.stack, "disks/patch/get-uuids", 3, 3, parameters.primaryGatewayConfig, parameters.nodes))
                .thenReturn(orchestratorStateParams);
        when(saltOrchestrator.applyOrchestratorState(orchestratorStateParams)).thenReturn(createSaltResponse("valid-saltresponse.json"));

        ExistingStackPatchApplyException exception = assertThrows(ExistingStackPatchApplyException.class, () -> underTest.doApply(parameters.stack));

        assertTrue(exception.getMessage().contains("Duplicate fstab lines with the same uuid, but different mountpoint"));
        verify(resourceNotifier, never()).notifyUpdates(anyList(), any());
        verify(saltOrchestrator, never()).updateMountDiskPillar(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void testSuccessfulDoApply() throws Exception {
        TestParameters parameters = setupDefaults(createDiskResources());
        when(volumeConnector.getVolumeDeviceMappingByInstance(parameters.cloudConnectResources.getAuthenticatedContext(),
                parameters.cloudConnectResources.getCloudStack())).thenReturn(
                Map.ofEntries(Map.entry("instance1",
                                Map.ofEntries(Map.entry("i1v1", "lun1"), Map.entry("i1v2", "lun2"), Map.entry("i1v3", "lun3"))),
                        Map.entry("instance2",
                                Map.ofEntries(Map.entry("i2v1", "lun1"), Map.entry("i2v2", "lun2"), Map.entry("i2v3", "lun3"), Map.entry("i2v4", "lun4"))),
                        Map.entry("instance3", Map.of())));
        OrchestratorStateParams orchestratorStateParams = new OrchestratorStateParams();
        when(saltStateParamsService.createStateParams(parameters.stack, "disks/patch/get-uuids", 3, 3, parameters.primaryGatewayConfig, parameters.nodes))
                .thenReturn(orchestratorStateParams);
        when(saltOrchestrator.applyOrchestratorState(orchestratorStateParams)).thenReturn(createSaltResponse("valid-saltresponse.json"));

        boolean accepted = underTest.doApply(parameters.stack);

        assertTrue(accepted);
        ArgumentCaptor<List<CloudResource>> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(resourceNotifier).notifyUpdates(cloudResourceArgumentCaptor.capture(), eq(parameters.cloudConnectResources.getCloudContext()));
        // Checking the patched resources
        List<CloudResource> patchedResources = cloudResourceArgumentCaptor.getValue();
        CloudResource instance1Resource = patchedResources.stream().filter(res -> res.getInstanceId().equals("instance1")).findFirst().get();
        VolumeSetAttributes vsa1 = instance1Resource.getTypedAttributes(VolumeSetAttributes.class, () -> new VolumeSetAttributes.Builder().build());
        for (int i = 0; i < vsa1.getVolumes().size(); i++) {
            VolumeSetAttributes.Volume volume = vsa1.getVolumes().get(i);
            assertEquals(volume.getId(), "i1v" + (i + 1));
            assertEquals(volume.getDevice(), "lun" + (i + 1));
        }
        assertEquals("uuid1 uuid2 uuid3", vsa1.getUuids());
        CloudResource instance2Resource = patchedResources.stream().filter(res -> res.getInstanceId().equals("instance2")).findFirst().get();
        VolumeSetAttributes vsa2 = instance2Resource.getTypedAttributes(VolumeSetAttributes.class, () -> new VolumeSetAttributes.Builder().build());
        for (int i = 0; i < vsa2.getVolumes().size(); i++) {
            VolumeSetAttributes.Volume volume = vsa2.getVolumes().get(i);
            assertEquals(volume.getId(), "i2v" + (i + 1));
            assertEquals(volume.getDevice(), "lun" + (i + 1));
            assertEquals(i == 3 ? CloudVolumeUsageType.DATABASE : CloudVolumeUsageType.GENERAL,  volume.getCloudVolumeUsageType());
        }
        assertEquals("uuid1 uuid2 uuid3 uuid4", vsa2.getUuids());
        CloudResource instance3Resource = patchedResources.stream().filter(res -> res.getInstanceId().equals("instance3")).findFirst().get();
        VolumeSetAttributes vsa3 = instance3Resource.getTypedAttributes(VolumeSetAttributes.class, () -> new VolumeSetAttributes.Builder().build());
        assertTrue(vsa3.getVolumes().isEmpty());
        assertNull(vsa3.getUuids());

        verify(saltOrchestrator).updateMountDiskPillar(eq(parameters.stack()), eq(parameters.gatewayConfigs), eq(parameters.nodes),
                any(), eq(CloudConstants.AZURE), eq(false));
    }

    @Test
    void testDoApplyWhenUpdateMountPillarThrowsException() throws Exception {
        TestParameters parameters = setupDefaults(createDiskResources());
        when(volumeConnector.getVolumeDeviceMappingByInstance(parameters.cloudConnectResources.getAuthenticatedContext(),
                parameters.cloudConnectResources.getCloudStack())).thenReturn(
                Map.ofEntries(Map.entry("instance1",
                                Map.ofEntries(Map.entry("i1v1", "lun1"), Map.entry("i1v2", "lun2"), Map.entry("i1v3", "lun3"))),
                        Map.entry("instance2",
                                Map.ofEntries(Map.entry("i2v1", "lun1"), Map.entry("i2v2", "lun2"), Map.entry("i2v3", "lun3"), Map.entry("i2v4", "lun4"))),
                        Map.entry("instance3", Map.of())));
        OrchestratorStateParams orchestratorStateParams = new OrchestratorStateParams();
        when(saltStateParamsService.createStateParams(parameters.stack, "disks/patch/get-uuids", 3, 3, parameters.primaryGatewayConfig, parameters.nodes))
                .thenReturn(orchestratorStateParams);
        when(saltOrchestrator.applyOrchestratorState(orchestratorStateParams)).thenReturn(createSaltResponse("valid-saltresponse.json"));
        doThrow(new RuntimeException("exception")).when(saltOrchestrator).updateMountDiskPillar(eq(parameters.stack()), eq(parameters.gatewayConfigs),
                eq(parameters.nodes), any(), eq(CloudConstants.AZURE), eq(false));

        ExistingStackPatchApplyException exception = assertThrows(ExistingStackPatchApplyException.class, () -> underTest.doApply(parameters.stack));

        assertEquals("Azure attached volumes patch on null failed: exception", exception.getMessage());
        ArgumentCaptor<List<CloudResource>> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(resourceNotifier, times(2)).notifyUpdates(cloudResourceArgumentCaptor.capture(), eq(parameters.cloudConnectResources.getCloudContext()));
        List<CloudResource> lastPersistedResources = cloudResourceArgumentCaptor.getValue();
        assertTrue(parameters.diskResources.size() == lastPersistedResources.size() &&
                parameters.diskResources.values().containsAll(lastPersistedResources));
    }

    private TestParameters setupDefaultsForFailures(String fstab) {
        VolumeSetAttributes volumeSetAttributes =
                createVolumeSetAttributes(fstab, List.of(new VolumeSetAttributes.Volume("id", "/dev/sdc", 10, "type", CloudVolumeUsageType.GENERAL)));
        Pair<Resource, CloudResource> volumeSetResource = createVolumeSetResource("instance1", volumeSetAttributes);
        return setupDefaults(Map.of(volumeSetResource.getKey(), volumeSetResource.getValue()));
    }

    private TestParameters setupDefaults(Map<Resource, CloudResource> diskResources) {
        Stack stack = new Stack();
        stack.setId(1L);
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        stack.setCluster(cluster);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);
        stack.setPlatformVariant(CloudConstants.AZURE);
        stack.setCloudPlatform(CloudPlatform.AZURE.name());
        stack.setResources(diskResources.keySet());
        GatewayConfig primaryGatewayConfig = GatewayConfig.builder().build();
        List<GatewayConfig> gatewayConfigs = List.of(primaryGatewayConfig);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(primaryGatewayConfig);

        Node node1 = createNode("instance1", "node1");
        Node node2 = createNode("instance2", "node2");
        Node node3 = createNode("instance3", "node3");
        Set<Node> nodes = Set.of(node1, node2, node3);
        when(stackUtil.collectNodes(stack)).thenReturn(nodes);
        lenient().when(stackUtil.collectNodesWithDiskData(stack)).thenReturn(nodes);
        NodeReachabilityResult nodeReachabilityResult = new NodeReachabilityResult(nodes, Set.of());
        when(saltOrchestrator.getResponsiveNodes(any(), any(), eq(Boolean.TRUE))).thenReturn(nodeReachabilityResult);

        CloudConnectResources cloudConnectResources = new CloudConnectResources(mock(CloudCredential.class), mock(CloudContext.class),
                mock(CloudConnector.class), mock(AuthenticatedContext.class), mock(CloudStack.class));
        when(cloudConnectorHelper.getCloudConnectorResources(stack)).thenReturn(cloudConnectResources);
        diskResources.forEach((key, value) -> when(resourceConverter.convert(key)).thenReturn(value));
        when(cloudConnectResources.getCloudConnector().volumeConnector()).thenReturn(volumeConnector);
        return new TestParameters(stack, primaryGatewayConfig, gatewayConfigs, cloudConnectResources, diskResources, nodes);
    }

    private Map<Resource, CloudResource> createDiskResources() throws Exception {
        Map<Resource, CloudResource> resourceMap = new HashMap<>();
        VolumeSetAttributes.Volume i1v1 = new VolumeSetAttributes.Volume("i1v1", "/dev/sdc", 10, "HDD", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i1v2 = new VolumeSetAttributes.Volume("i1v2", "/dev/sdd", 10, "HDD", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i1v3 = new VolumeSetAttributes.Volume("i1v3", "/dev/sde", 10, "HDD", CloudVolumeUsageType.GENERAL);
        Pair<Resource, CloudResource> volumeSetResource1 = createVolumeSetResource("instance1",
                createVolumeSetAttributes(createFstab("valid-fstab-instance1.txt"), List.of(i1v1, i1v2, i1v3)));

        VolumeSetAttributes.Volume i2v1 = new VolumeSetAttributes.Volume("i2v1", "/dev/sdc", 10, "HDD", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i2v2 = new VolumeSetAttributes.Volume("i2v2", "/dev/sdd", 10, "HDD", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i2v3 = new VolumeSetAttributes.Volume("i2v3", "/dev/sde", 10, "HDD", CloudVolumeUsageType.DATABASE);
        VolumeSetAttributes.Volume i2v4 = new VolumeSetAttributes.Volume("i2v4", "/dev/sdf", 10, "HDD", CloudVolumeUsageType.GENERAL);
        Pair<Resource, CloudResource> volumeSetResource2 = createVolumeSetResource("instance2",
                createVolumeSetAttributes(createFstab("valid-fstab-instance2.txt"), List.of(i2v1, i2v2, i2v3, i2v4)));

        Pair<Resource, CloudResource> volumeSetResource3 = createVolumeSetResource("instance3", createVolumeSetAttributes("", List.of()));

        resourceMap.put(volumeSetResource1.getKey(), volumeSetResource1.getValue());
        resourceMap.put(volumeSetResource2.getKey(), volumeSetResource2.getValue());
        resourceMap.put(volumeSetResource3.getKey(), volumeSetResource3.getValue());
        return resourceMap;
    }

    private List<Map<String, JsonNode>> createSaltResponse(String responseFile) throws Exception {
        return JsonUtil.readValue(FileReaderUtil.readResourceFile(this, responseFile), new TypeReference<>() {
        });
    }

    private String createFstab(String fstabFile) throws Exception {
        return FileReaderUtil.readResourceFile(this, fstabFile);
    }

    private Node createNode(String instanceId, String fqdn) {
        return new Node("", "", instanceId, "", fqdn, "");
    }

    private Pair<Resource, CloudResource> createVolumeSetResource(String instanceId, VolumeSetAttributes volumeSetAttributes) {
        Resource resource = new Resource();
        resource.setId(1L);
        resource.setInstanceId(instanceId);
        resource.setAttributes(new Json(volumeSetAttributes));
        resource.setResourceType(ResourceType.AZURE_VOLUMESET);

        CloudResource cloudResource = CloudResource.builder()
                .withInstanceId(instanceId)
                .withType(ResourceType.AZURE_VOLUMESET)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withParameters(new HashMap<>())
                .build();
        cloudResource.setTypedAttributes(volumeSetAttributes);

        return Pair.of(resource, cloudResource);
    }

    private VolumeSetAttributes createVolumeSetAttributes(String fstab, List<VolumeSetAttributes.Volume> volumes) {
        return new VolumeSetAttributes.Builder()
                .withVolumes(volumes)
                .withFstab(fstab)
                .build();
    }

    private record TestParameters(
            Stack stack,
            GatewayConfig primaryGatewayConfig,
            List<GatewayConfig> gatewayConfigs,
            CloudConnectResources cloudConnectResources,
            Map<Resource, CloudResource> diskResources,
            Set<Node> nodes) {
    }
}
