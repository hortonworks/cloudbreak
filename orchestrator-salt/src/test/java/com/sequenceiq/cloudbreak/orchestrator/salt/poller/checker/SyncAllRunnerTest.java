package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SaltStates.class)
public class SyncAllRunnerTest {

    @Test
    public void submit() {
        Set<String> targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");
        Set<Node> allNode = new HashSet<>();
        allNode.add(new Node("10.0.0.1", "5.5.5.1", "10-0-0-1.example.com", "hg"));
        allNode.add(new Node("10.0.0.2", "5.5.5.2", "10-0-0-2.example.com", "hg"));
        allNode.add(new Node("10.0.0.3", "5.5.5.3", "10-0-0-3.example.com", "hg"));

        PowerMockito.mockStatic(SaltStates.class);
        ApplyResponse applyResponse = new ApplyResponse();
        List<Map<String, JsonNode>> result = new ArrayList<>();
        Map<String, JsonNode> nodes = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        nodes.put("10-0-0-1.example.com", objectMapper.valueToTree("something"));
        nodes.put("10-0-0-2.example.com", objectMapper.valueToTree("something"));
        result.add(nodes);
        applyResponse.setResult(result);
        PowerMockito.when(SaltStates.syncAll(any())).thenReturn(applyResponse);

        SyncAllRunner syncAllRunner = new SyncAllRunner(targets, allNode);

        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);
        String missingIps = syncAllRunner.submit(saltConnector);
        assertThat(syncAllRunner.getTarget(), hasItems("10.0.0.3"));
        assertEquals("[10.0.0.3]", missingIps);
    }

}