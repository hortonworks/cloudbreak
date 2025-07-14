package com.sequenceiq.cloudbreak.core.flow2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudConnectResources;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.converter.spi.CloudResourceToResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.ConfigUpdateUtilService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.util.CloudConnectorHelper;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AddVolumesServiceTest {

    @Mock
    private StackUtil stackUtil;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Mock
    private CloudResourceToResourceConverter cloudResourceToResourceConverter;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private StackService stackService;

    @Mock
    private CloudConnectorHelper cloudConnectorHelper;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private CmTemplateProcessor processor;

    @Mock
    private ConfigUpdateUtilService configUpdateUtilService;

    @Mock
    private ClusterBootstrapper clusterBootstrapper;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private AwsResourceVolumeConnector awsResourceVolumeConnector;

    @Mock
    private ResourceService resourceService;

    @Mock
    private DeleteVolumesService deleteVolumesService;

    @InjectMocks
    private AddVolumesService underTest;

    @Mock
    private Stack stack;

    @Mock
    private Node node;

    @Mock
    private List<GatewayConfig> gatewayConfigs;

    @Mock
    private Cluster cluster;

    @Mock
    private Resource resource;

    @Mock
    private VolumeSetAttributes.Volume volume;

    @Mock
    private StackDto stackDto;

    @Mock
    private CloudResource cloudResource;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private AuthenticatedContext authenticatedContext;

    private Set<Node> nodes = new HashSet<>();

    @BeforeEach
    void setUp() {
        lenient().when(node.getHostGroup()).thenReturn("test");
        nodes.add(node);
        lenient().when(stack.getCluster()).thenReturn(cluster);
        lenient().when(cluster.getId()).thenReturn(1L);
        lenient().when(stackUtil.collectNodes(stack)).thenReturn(nodes);
        lenient().when(stackUtil.collectNodesWithDiskData(stack)).thenReturn(nodes);
        lenient().when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        Blueprint bp = mock(Blueprint.class);
        lenient().when(stack.getBlueprint()).thenReturn(bp);
        lenient().when(stack.getId()).thenReturn(1L);
        lenient().when(bp.getBlueprintJsonText()).thenReturn("test");
        lenient().when(cmTemplateProcessorFactory.get("test")).thenReturn(processor);
        lenient().when(stackDtoService.getById(1L)).thenReturn(stackDto);
        lenient().when(stackDto.getCloudPlatform()).thenReturn("AWS");
        lenient().when(stackDto.getPlatformVariant()).thenReturn("AWS");

        CloudConnectResources cloudConnectorResources = new CloudConnectResources(null, null, cloudConnector, authenticatedContext, cloudStack);
        lenient().when(cloudConnectorHelper.getCloudConnectorResources(stackDto)).thenReturn(cloudConnectorResources);
        lenient().when(resourceToCloudResourceConverter.convert(resource)).thenReturn(cloudResource);
        lenient().when(cloudConnector.volumeConnector()).thenReturn(awsResourceVolumeConnector);

        Group group = mock(Group.class);
        lenient().when(group.getName()).thenReturn("test");
        lenient().when(cloudStack.getGroups()).thenReturn(List.of(group));
    }

    @Test
    void testRedeployStatesAndMountDisks() throws Exception {
        Map<String, Map<String, String>> fstabInformation = Map.of("test", Map.of("fstab", "test-fstab", "uuid", "123"));
        doReturn(Map.of("test", Set.of(ServiceComponent.of("TEST", "TEST")))).when(processor).getServiceComponentsByHostGroup();
        doReturn(fstabInformation).when(hostOrchestrator).formatAndMountDisksAfterModifyingVolumesOnNodes(eq(gatewayConfigs), eq(nodes),
                eq(nodes), any());
        Map<String, Map<String, String>> response = underTest.redeployStatesAndMountDisks(stack, "test");
        verify(hostOrchestrator).formatAndMountDisksAfterModifyingVolumesOnNodes(eq(gatewayConfigs), eq(nodes), eq(nodes), any());
        assertEquals("test-fstab", response.get("test").get("fstab"));
    }

    @Test
    void testRedeployStatesAndMountDisksThrowsException() throws Exception {
        doReturn(Map.of("test", Set.of(ServiceComponent.of("TEST", "TEST")))).when(processor).getServiceComponentsByHostGroup();
        doThrow(new CloudbreakOrchestratorFailedException("test")).when(hostOrchestrator).formatAndMountDisksAfterModifyingVolumesOnNodes(any(), any(),
                any(), any());
        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.redeployStatesAndMountDisks(stack, "test"));
        verify(hostOrchestrator).formatAndMountDisksAfterModifyingVolumesOnNodes(eq(gatewayConfigs), eq(nodes), eq(nodes), any());
        assertEquals("test", exception.getMessage());
    }

    @Test
    void testValidateVolumeAddition() {
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        InstanceMetaData instanceMetaData1 = mock(InstanceMetaData.class);
        InstanceMetaData instanceMetaData2 = mock(InstanceMetaData.class);
        Template template = mock(Template.class);
        VolumeTemplate volumeTemplate1 = mock(VolumeTemplate.class);
        VolumeTemplate volumeTemplate2 = mock(VolumeTemplate.class);
        when(instanceMetaData1.getInstanceId()).thenReturn("instanceId1");
        when(instanceMetaData2.getInstanceId()).thenReturn("instanceId2");
        when(instanceGroup.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(instanceMetaData1, instanceMetaData2));
        when(instanceGroupService.getInstanceGroupWithTemplateAndInstancesByGroupNameInStack(1L, "test")).thenReturn(Optional.of(instanceGroup));
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(template.getVolumeTemplates()).thenReturn(Set.of(volumeTemplate1, volumeTemplate2));
        when(volumeTemplate1.getVolumeCount()).thenReturn(1);
        when(volumeTemplate2.getVolumeCount()).thenReturn(2);
        when(awsResourceVolumeConnector.getAttachedVolumeCountPerInstance(eq(authenticatedContext), eq(cloudStack), anyList()))
                .thenReturn(Map.of("instanceId1", 3, "instanceId2", 4));

        assertThrows(CloudbreakServiceException.class, () -> underTest.validateVolumeAddition(1L, "test"));
    }

    @Test
    void testCreateAndAttachVolumes() throws CloudbreakServiceException {
        CloudResource cloudResourceResponse = mock(CloudResource.class);
        Resource convertedResponse = mock(Resource.class);
        doReturn(List.of(cloudResourceResponse)).when(awsResourceVolumeConnector).createVolumes(any(), any(), any(), any(), eq(2), any());
        doReturn("test").when(convertedResponse).getInstanceGroup();
        doReturn(convertedResponse).when(cloudResourceToResourceConverter).convert(cloudResourceResponse);
        List<Resource> response = underTest.createVolumes(Set.of(resource), volume, 2, "test", 1L);
        verify(awsResourceVolumeConnector).createVolumes(any(), any(), eq(volume), eq(cloudStack), eq(2), eq(List.of(cloudResource)));
        assertEquals("test", response.get(0).getInstanceGroup());
    }

    @Test
    void testCreateAndAttachVolumesThrowsException() throws CloudbreakServiceException {
        doThrow(new CloudbreakServiceException("test")).when(awsResourceVolumeConnector).createVolumes(any(), any(), any(), any(), eq(2), any());
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.createVolumes(Set.of(resource),
                volume, 2, "test", 1L));
        verify(awsResourceVolumeConnector).createVolumes(any(), any(), eq(volume), eq(cloudStack), eq(2), eq(List.of(cloudResource)));
        assertEquals("test", exception.getMessage());
    }

    @Test
    void testUpdateResourceVolumeStatus() {
        VolumeSetAttributes attributes = mock(VolumeSetAttributes.class);
        volume = new VolumeSetAttributes.Volume("", "/dev/xvda", 400, "gp2", CloudVolumeUsageType.GENERAL);
        doReturn(List.of(volume)).when(attributes).getVolumes();
        doReturn(Optional.of(attributes)).when(resourceAttributeUtil).getTypedAttributes(any(), eq(VolumeSetAttributes.class));
        Set<Resource> resourceSet = Set.of(resource);

        underTest.updateResourceVolumeStatus(resourceSet, CloudVolumeStatus.CREATED);

        verify(resourceService).saveAll(resourceSet);
        assertEquals(volume.getCloudVolumeStatus(), CloudVolumeStatus.CREATED);
    }

    @Test
    void testRollbackCreatedVolumes() throws Exception {
        Resource resourceInput = new Resource(ResourceType.AWS_VOLUMESET, "test_resource", stack, "az");

        VolumeSetAttributes.Volume volume1 = new VolumeSetAttributes.Volume("1", "/dev/xvda", 400, "gp2", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume volume2 = new VolumeSetAttributes.Volume("2", "/dev/xvda", 400, "gp2", CloudVolumeUsageType.GENERAL);
        volume2.setCloudVolumeStatus(CloudVolumeStatus.CREATED);
        VolumeSetAttributes attributes = new VolumeSetAttributes("az", false, "", List.of(volume1, volume2), 400, "gp2");
        Json attributesJson = convertAttributesToJson(attributes);
        resourceInput.setAttributes(attributesJson);
        resourceInput.setInstanceId("instance-id-1");
        resourceInput.setInstanceGroup("instance-group");

        Resource stackResource = new Resource(ResourceType.AWS_VOLUMESET, "test_resource", stack, "az");
        VolumeSetAttributes stackAttributes = new VolumeSetAttributes("az", false, "", List.of(volume1), 400, "gp2");
        Json stackAttributesJson = convertAttributesToJson(stackAttributes);
        stackResource.setAttributes(stackAttributesJson);
        stackResource.setInstanceId("instance-id-1");
        stackResource.setInstanceGroup("instance-group");
        Set<Resource> resourceSet = Set.of(stackResource);
        doReturn(resourceSet).when(stackDto).getResources();
        doCallRealMethod().when(resourceAttributeUtil).getTypedAttributes(any(), any());
        doReturn(cloudResource).when(resourceToCloudResourceConverter).convert(stackResource);

        underTest.rollbackCreatedVolumes(Set.of(resourceInput), 1L);

        ArgumentCaptor<Set<Resource>> savedResourceArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        verify(resourceService).saveAll(savedResourceArgumentCaptor.capture());
        assertEquals(1, savedResourceArgumentCaptor.getValue().size());
        List<Resource> savedResources = new ArrayList<>(savedResourceArgumentCaptor.getValue());
        VolumeSetAttributes expectedSavedAttributes = new VolumeSetAttributes("az", false, "", List.of(volume1), 400, "gp2");
        Json expectedSavedJson = convertAttributesToJson(expectedSavedAttributes);
        assertEquals(expectedSavedJson, savedResources.get(0).getAttributes());

        ArgumentCaptor<Resource> deletedResourceArgumentCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceToCloudResourceConverter, times(1)).convert(deletedResourceArgumentCaptor.capture());
        VolumeSetAttributes expectedDeletedAttributes = new VolumeSetAttributes("az", false, "", List.of(volume2), 400, "gp2");
        Json expectedDeletedJson = convertAttributesToJson(expectedDeletedAttributes);
        assertEquals(expectedDeletedJson, deletedResourceArgumentCaptor.getValue().getAttributes());

        verify(deleteVolumesService).detachResources(eq(List.of(cloudResource)), any(CloudPlatformVariant.class), eq(authenticatedContext));
        verify(deleteVolumesService).deleteResources(eq(List.of(cloudResource)), any(CloudPlatformVariant.class), eq(authenticatedContext));
    }

    private static Json convertAttributesToJson(VolumeSetAttributes attributes) {
        try {
            return new Json(attributes);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Cannot parse resource attributes");
        }
    }
}
