package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SaltStates.class)
public class StateRunnerTest {

    private Set<String> targets;

    private Set<Node> allNode;

    @Test
    public void submit() throws Exception {
        targets = new HashSet<>();
        targets.add("10-0-0-1.example.com");
        allNode = new HashSet<>();
        allNode.add(new Node("10.0.0.1", "5.5.5.1", "10-0-0-1.example.com", "hg"));
        allNode.add(new Node("10.0.0.2", "5.5.5.2", "10-0-0-2.example.com", "hg"));
        allNode.add(new Node("10.0.0.3", "5.5.5.3", "10-0-0-3.example.com", "hg"));

        StateRunner stateRunner = new StateRunner(targets, allNode, "example_state");

        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);

        PowerMockito.mockStatic(SaltStates.class);
        String jobId = "1";

        ApplyResponse applyResponse = new ApplyResponse();
        List<Map<String, JsonNode>> result = new ArrayList<>();
        Map<String, JsonNode> nodes = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        nodes.put("jid", objectMapper.valueToTree(jobId));
        result.add(nodes);
        applyResponse.setResult(result);
        PowerMockito.when(SaltStates.applyState(any(), any(), any())).thenReturn(applyResponse);


        String jid = stateRunner.submit(saltConnector);
        assertEquals(jobId, jid);
        PowerMockito.verifyStatic(SaltStates.class);
        ArgumentCaptor acHostList = ArgumentCaptor.forClass(HostList.class);
        SaltStates.applyState(eq(saltConnector), eq("example_state"), (Target<String>) acHostList.capture());
        assertThat(acHostList.getValue(), IsInstanceOf.instanceOf(HostList.class));
        HostList hostList = (HostList) acHostList.getValue();
        assertEquals("10-0-0-1.example.com", hostList.getTarget());
    }

}