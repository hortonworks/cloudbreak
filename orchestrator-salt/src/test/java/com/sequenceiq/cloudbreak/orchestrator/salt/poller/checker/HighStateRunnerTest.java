package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SaltStates.class)
public class HighStateRunnerTest {

    private Set<String> targets;

    private Set<Node> allNode;

    @Test
    public void submit() {
        String target1 = "10.0.0.1";
        String target2 = "10.0.0.2";
        targets = new HashSet<>();
        targets.add(target1);
        targets.add(target2);
        allNode = new HashSet<>();
        allNode.add(new Node("10.0.0.1", "5.5.5.1", "10-0-0-1.example.com", "hg"));
        allNode.add(new Node("10.0.0.2", "5.5.5.2", "10-0-0-2.example.com", "hg"));
        allNode.add(new Node("10.0.0.3", "5.5.5.3", "10-0-0-3.example.com", "hg"));

        HighStateRunner highStateRunner = new HighStateRunner(targets, allNode);

        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);

        PowerMockito.mockStatic(SaltStates.class);
        String jobId = "1";
        PowerMockito.when(SaltStates.highstate(any(), any())).thenReturn(jobId);

        String jid = highStateRunner.submit(saltConnector);
        assertEquals(jobId, jid);
        PowerMockito.verifyStatic(SaltStates.class);
        ArgumentCaptor<Target<String>> targetCaptor = ArgumentCaptor.forClass(Target.class);
        SaltStates.highstate(eq(saltConnector), targetCaptor.capture());
        String order1 = target1 + "," + target2;
        String order2 = target2 + "," + target1;
        String actual = targetCaptor.getValue().getTarget();
        assertTrue(order1.equals(actual) || order2.equals(actual));
    }

    @Test
    public void stateType() {
        assertEquals(StateType.HIGH, new HighStateRunner(targets, allNode).stateType());
    }

}