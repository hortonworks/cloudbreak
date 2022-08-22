package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

@ExtendWith(MockitoExtension.class)
class StateRunnerTest {

    @Mock
    private SaltStateService saltStateService;

    @Mock
    private SaltConnector saltConnector;

    private Set<String> targets;

    @Test
    void submit() throws Exception {
        targets = new HashSet<>();
        targets.add("10-0-0-1.example.com");

        StateRunner stateRunner = new StateRunner(saltStateService, targets, "example_state");

        String jobId = "1";

        ApplyResponse applyResponse = new ApplyResponse();
        List<Map<String, JsonNode>> result = new ArrayList<>();
        Map<String, JsonNode> nodes = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        nodes.put("jid", objectMapper.valueToTree(jobId));
        result.add(nodes);
        applyResponse.setResult(result);
        when(saltStateService.applyState(any(), any(), any())).thenReturn(applyResponse);


        String jid = stateRunner.submit(saltConnector);
        assertEquals(jobId, jid);
        ArgumentCaptor<HostList> acHostList = ArgumentCaptor.forClass(HostList.class);
        verify(saltStateService).applyState(eq(saltConnector), eq("example_state"), acHostList.capture());
        assertThat(acHostList.getValue(), IsInstanceOf.instanceOf(HostList.class));
        HostList hostList = acHostList.getValue();
        assertEquals("10-0-0-1.example.com", hostList.getTarget());
    }

}
