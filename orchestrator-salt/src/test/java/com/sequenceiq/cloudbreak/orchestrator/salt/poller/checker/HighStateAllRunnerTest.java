package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

@ExtendWith(MockitoExtension.class)
class HighStateAllRunnerTest {

    @Mock
    private SaltStateService saltStateService;

    @Mock
    private SaltConnector saltConnector;

    private Set<String> targets;

    private Set<Node> allNode;

    @Test
    void submit() {
        targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");
        allNode = new HashSet<>();
        allNode.add(new Node("10.0.0.1", "5.5.5.1", "10-0-0-1.example.com", "hg"));
        allNode.add(new Node("10.0.0.2", "5.5.5.2", "10-0-0-2.example.com", "hg"));
        allNode.add(new Node("10.0.0.3", "5.5.5.3", "10-0-0-3.example.com", "hg"));

        HighStateAllRunner highStateAllRunner = new HighStateAllRunner(saltStateService, targets, allNode);

        String jobId = "1";
        ApplyResponse applyResponse = mock(ApplyResponse.class);
        when(applyResponse.getJid()).thenReturn(jobId);
        when(saltStateService.highstate(any())).thenReturn(applyResponse);

        String jid = highStateAllRunner.submit(saltConnector);
        assertEquals(jobId, jid);
        verify(saltStateService).highstate(eq(saltConnector));
    }

    @Test
    void stateType() {
        assertEquals(StateType.HIGH, new HighStateAllRunner(saltStateService, targets, allNode).stateType());
    }

}