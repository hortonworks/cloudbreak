package com.sequenceiq.cloudbreak.conclusion.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
class SaltMinionCheckerConclusionStepTest {

    @Mock
    private StackService stackService;

    @Mock
    private StackUtil stackUtil;

    @InjectMocks
    private SaltMinionCheckerConclusionStep underTest;

    @Test
    public void checkShouldBeSuccessfulIfNoUnreachableNodeFound() throws NodesUnreachableException {
        when(stackService.getByIdWithListsInTransaction(eq(1L))).thenReturn(new Stack());
        Set<Node> nodes = Set.of(createNode("host1"), createNode("host2"));
        when(stackUtil.collectNodes(any())).thenReturn(nodes);
        when(stackUtil.collectAndCheckReachableNodes(any(), anyCollection())).thenReturn(nodes);
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(SaltMinionCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(stackService, times(1)).getByIdWithListsInTransaction(eq(1L));
        verify(stackUtil, times(1)).collectNodes(any());
        verify(stackUtil, times(1)).collectAndCheckReachableNodes(any(), any());
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfUnreachableNodeFound() throws NodesUnreachableException {
        when(stackService.getByIdWithListsInTransaction(eq(1L))).thenReturn(new Stack());
        when(stackUtil.collectNodes(any())).thenReturn(Set.of(createNode("host1"), createNode("host2")));
        when(stackUtil.collectAndCheckReachableNodes(any(), anyCollection())).thenThrow(new NodesUnreachableException("error", Set.of("host1")));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("Unreachable nodes: [host1]. Please check the instances on your cloud provider for further details.", stepResult.getConclusion());
        assertEquals("Unreachable salt minions: [host1]", stepResult.getDetails());
        assertEquals(SaltMinionCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(stackService, times(1)).getByIdWithListsInTransaction(eq(1L));
        verify(stackUtil, times(1)).collectNodes(any());
        verify(stackUtil, times(1)).collectAndCheckReachableNodes(any(), any());
    }

    private Node createNode(String fqdn) {
        return new Node("privateIp", "publicIp", "instanceId", "instanceType", fqdn, "hostGroup");
    }

}