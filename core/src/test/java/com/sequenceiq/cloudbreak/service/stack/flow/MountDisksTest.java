package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.diskvalidator.DiskValidator;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.type.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class MountDisksTest {

    @Mock
    private StackService stackService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private ResourceService resourceService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private DiskValidator diskValidator;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private MountDisks underTest;

    @Mock
    private Stack stack;

    @Captor
    private ArgumentCaptor<Set<Node>> targetsCaptor;

    @Captor
    private ArgumentCaptor<Set<Node>> allNodesCaptor;

    @Test
    public void mountDisksOnNewNodesShouldUseReachableNodes() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        when(entitlementService.isXfsForEphemeralDisksSupported(any())).thenReturn(Boolean.FALSE);
        Set<String> newNodeAddresses = Set.of("node-1");
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(stack.getPlatformVariant()).thenReturn(CloudConstants.MOCK);
        when(stack.getCluster()).thenReturn(new Cluster());
        when(stack.getDiskResourceType()).thenReturn(ResourceType.MOCK_VOLUME);
        when(stack.getResourceCrn()).thenReturn(TestUtil.STACK_CRN);
        Node node1 = new Node("1.1.1.1", "1.1.1.1", "id1", "m5.xlarge", "node-1", "worker");
        Node node2 = new Node("1.1.1.2", "1.1.1.2", "id2", "m5.xlarge", "node-2", "worker");
        Set<Node> reachableNodes = new HashSet<>();
        reachableNodes.add(node1);
        reachableNodes.add(node2);
        Set<Node> newNodesWithDiskData = new HashSet<>();
        newNodesWithDiskData.add(node1);
        when(stackUtil.collectNewNodesWithDiskData(stack, newNodeAddresses)).thenReturn(newNodesWithDiskData);

        underTest.mountDisksOnNewNodes(1L, newNodeAddresses, reachableNodes);
        verify(stackUtil).collectNewNodesWithDiskData(stack, newNodeAddresses);
        verify(hostOrchestrator).formatAndMountDisksOnNodes(any(), any(), targetsCaptor.capture(), allNodesCaptor.capture(), any());
        verify(diskValidator, times(1)).validateDisks(stack, newNodesWithDiskData);
        Set<Node> capturedTargets = targetsCaptor.getValue();
        Set<Node> capturedAllNode = allNodesCaptor.getValue();
        assertTrue(capturedTargets.contains(node1));
        assertFalse(capturedTargets.contains(node2));
        assertTrue(capturedAllNode.contains(node1));
        assertTrue(capturedAllNode.contains(node2));
    }

}