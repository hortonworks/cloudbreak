package com.sequenceiq.cloudbreak.core.flow2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.converter.spi.CloudResourceToResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.ConfigUpdateUtilService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.CloudConnectResources;
import com.sequenceiq.cloudbreak.util.CloudConnectorHelper;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
public class AddVolumesServiceTest {

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
    private AwsResourceVolumeConnector awsResourceVolumeConnector;

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
    public void setUp() {
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
        lenient().when(bp.getBlueprintText()).thenReturn("test");
        lenient().when(cmTemplateProcessorFactory.get("test")).thenReturn(processor);
        lenient().when(stackDtoService.getById(1L)).thenReturn(mock(StackDto.class));

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
        doReturn(fstabInformation).when(hostOrchestrator).formatAndMountDisksAfterModifyingVolumesOnNodes(eq(gatewayConfigs), eq(nodes),
                eq(nodes), any());
        Map<String, Map<String, String>> response = underTest.redeployStatesAndMountDisks(stack, "test");
        verify(hostOrchestrator).formatAndMountDisksAfterModifyingVolumesOnNodes(eq(gatewayConfigs), eq(nodes), eq(nodes), any());
        assertEquals("test-fstab", response.get("test").get("fstab"));
    }

    @Test
    void testRedeployStatesAndMountDisksThrowsException() throws Exception  {
        doThrow(new CloudbreakOrchestratorFailedException("test")).when(hostOrchestrator).formatAndMountDisksAfterModifyingVolumesOnNodes(any(), any(),
                any(), any());
        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.redeployStatesAndMountDisks(stack, "test"));
        verify(hostOrchestrator).formatAndMountDisksAfterModifyingVolumesOnNodes(eq(gatewayConfigs), eq(nodes), eq(nodes), any());
        assertEquals("test", exception.getMessage());
    }

    @Test
    void testCreateAndAttachVolumes() throws CloudbreakServiceException {
        CloudResource cloudResourceResponse = mock(CloudResource.class);
        Resource convertedResponse = mock(Resource.class);
        doReturn(List.of(cloudResourceResponse)).when(awsResourceVolumeConnector).createVolumes(any(), any(), any(), any(), eq(2), any());
        doReturn("test").when(convertedResponse).getInstanceGroup();
        doReturn(convertedResponse).when(cloudResourceToResourceConverter).convert(cloudResourceResponse);
        doReturn(stackDto).when(stackDtoService).getById(eq(1L));
        List<Resource> response = underTest.createVolumes(Set.of(resource), volume, 2, "test", 1L);
        verify(awsResourceVolumeConnector).createVolumes(any(), any(), eq(volume), eq(cloudStack), eq(2), eq(List.of(cloudResource)));
        assertEquals("test", response.get(0).getInstanceGroup());
    }

    @Test
    void testCreateAndAttachVolumesThrowsException() throws CloudbreakServiceException  {
        doReturn(stackDto).when(stackDtoService).getById(eq(1L));
        doThrow(new CloudbreakServiceException("test")).when(awsResourceVolumeConnector).createVolumes(any(), any(), any(), any(), eq(2), any());
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.createVolumes(Set.of(resource),
                volume, 2, "test", 1L));
        verify(awsResourceVolumeConnector).createVolumes(any(), any(), eq(volume), eq(cloudStack), eq(2), eq(List.of(cloudResource)));
        assertEquals("test", exception.getMessage());
    }
}
