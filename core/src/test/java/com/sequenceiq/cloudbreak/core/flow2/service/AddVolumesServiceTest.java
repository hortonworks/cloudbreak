package com.sequenceiq.cloudbreak.core.flow2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
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

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.ConfigUpdateUtilService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
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
    private StackDtoService stackDtoService;

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
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private CmTemplateProcessor processor;

    @Mock
    private ConfigUpdateUtilService configUpdateUtilService;

    @Mock
    private ClusterBootstrapper clusterBootstrapper;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    private Set<Node> nodes = new HashSet<>();

    @BeforeEach
    public void setUp() {
        doReturn("test").when(node).getHostGroup();
        nodes.add(node);
        doReturn(cluster).when(stack).getCluster();
        doReturn(1L).when(cluster).getId();
        doReturn(nodes).when(stackUtil).collectNodes(stack);
        doReturn(nodes).when(stackUtil).collectNodesWithDiskData(stack);
        doReturn(gatewayConfigs).when(gatewayConfigService).getAllGatewayConfigs(stack);
        Blueprint bp = mock(Blueprint.class);
        doReturn(bp).when(stack).getBlueprint();
        doReturn(1L).when(stack).getId();
        doReturn("test").when(bp).getBlueprintText();
        doReturn(processor).when(cmTemplateProcessorFactory).get("test");
        doReturn(mock(StackDto.class)).when(stackDtoService).getById(1L);
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
}
