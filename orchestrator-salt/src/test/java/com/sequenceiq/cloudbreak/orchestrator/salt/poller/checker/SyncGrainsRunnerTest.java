package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SaltStates.class)
public class SyncGrainsRunnerTest {

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

        PowerMockito.mockStatic(SaltStates.class);
        ApplyResponse applyResponse = new ApplyResponse();
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> nodes = new HashMap<>();
        nodes.put("10-0-0-1.example.com", "something");
        nodes.put("10-0-0-2.example.com", "something");
        result.add(nodes);
        applyResponse.setResult(result);
        PowerMockito.when(SaltStates.syncGrains(any())).thenReturn(applyResponse);

        SyncGrainsRunner syncGrainsRunner = new SyncGrainsRunner(targets, allNode);

        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);
        String missingIps = syncGrainsRunner.submit(saltConnector);
        assertThat(syncGrainsRunner.getTarget(), hasItems("10.0.0.3"));
        assertEquals("[10.0.0.3]", missingIps);
    }

}