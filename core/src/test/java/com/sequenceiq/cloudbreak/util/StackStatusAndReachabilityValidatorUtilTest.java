package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@ExtendWith(MockitoExtension.class)
public class StackStatusAndReachabilityValidatorUtilTest {

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private SaltOrchestrator saltOrchestrator;

    @InjectMocks
    private StackStatusAndReachabilityValidatorUtil underTest;

    @Test
    public void testValidateStackStatusAndReachabilityWhenStatusIsNotAllowed() {
        Stack stack = mock(Stack.class);
        when(stack.getStatus()).thenReturn(Status.CREATE_IN_PROGRESS);

        boolean result = underTest.validateStackStatusAndReachability(stack);

        assertFalse(result);
    }

    @Test
    public void testValidateStackStatusAndReachabilityWhenNotAllNodesAreReachable() {
        Stack stack = mock(Stack.class);
        when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        Node node1 = mock(Node.class);
        Node node2 = mock(Node.class);
        when(stackUtil.collectNodes(stack)).thenReturn(Set.of(node1, node2));
        NodeReachabilityResult pingResult = new NodeReachabilityResult(Collections.emptySet(), Collections.emptySet());
        when(saltOrchestrator.getResponsiveNodes(Set.of(node1, node2), gatewayConfig, true)).thenReturn(pingResult);

        boolean result = underTest.validateStackStatusAndReachability(stack);

        assertFalse(result);
    }

    @Test
    public void testValidateStackStatusAndReachabilityWhenStatusIsAllowedAndAllNodesAreReachable() {
        Stack stack = mock(Stack.class);
        when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        Node node1 = mock(Node.class);
        Node node2 = mock(Node.class);
        Set<Node> allNodes = Set.of(node1, node2);
        when(stackUtil.collectNodes(stack)).thenReturn(allNodes);
        NodeReachabilityResult pingResult = new NodeReachabilityResult(allNodes, Collections.emptySet());
        when(saltOrchestrator.getResponsiveNodes(allNodes, gatewayConfig, true)).thenReturn(pingResult);

        boolean result = underTest.validateStackStatusAndReachability(stack);

        assertTrue(result);
    }
}
