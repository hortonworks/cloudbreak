package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SaltStates.class)
public class HighStateRunnerTest {

    private Set<String> targets;
    private Set<Node> allNode;

    @Test
    public void submit() {
        targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");
        allNode = new HashSet<>();
        allNode.add(new Node("10.0.0.1", "5.5.5.1", "10-0-0-1.example.com"));
        allNode.add(new Node("10.0.0.2", "5.5.5.2", "10-0-0-2.example.com"));
        allNode.add(new Node("10.0.0.3", "5.5.5.3", "10-0-0-3.example.com"));

        HighStateRunner highStateRunner = new HighStateRunner(targets, allNode);

        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);

        PowerMockito.mockStatic(SaltStates.class);
        String jobId = "1";
        PowerMockito.when(SaltStates.highstate(any())).thenReturn(jobId);

        String jid = highStateRunner.submit(saltConnector);
        assertEquals(jobId, jid);
        PowerMockito.verifyStatic();
        SaltStates.highstate(eq(saltConnector));
    }

    @Test
    public void stateType() {
        assertEquals(StateType.HIGH, new HighStateRunner(targets, allNode).stateType());
    }

}