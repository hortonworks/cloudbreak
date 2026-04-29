package com.sequenceiq.cloudbreak.orchestrator.salt.poller.join;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dyngr.core.AttemptState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionIpAddressesResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

class MinionDeletionPollerTest {

    private final SaltConnector saltConnector = mock(SaltConnector.class);

    private final SaltStateService saltStateService = mock(SaltStateService.class);

    @BeforeEach
    void setUp() {
        when(saltConnector.getHostname()).thenReturn("master.example.com");
    }

    @Test
    void processShouldContinueWhenMinionIsPresentAndReachable() {
        MinionIpAddressesResponse response = new MinionIpAddressesResponse();
        Map<String, JsonNode> responseByMinion = new HashMap<>();
        responseByMinion.put("m1.d", JsonNodeFactory.instance.arrayNode().add("10.0.0.2"));
        response.setResult(List.of(responseByMinion));
        when(saltStateService.collectMinionIpAddresses(eq(saltConnector), eq(Optional.of(Set.of("m1.d"))))).thenReturn(response);

        MinionDeletionPoller underTest = new MinionDeletionPoller(saltConnector, Set.of("m1.d"), saltStateService);

        assertEquals(AttemptState.CONTINUE, underTest.process().getState());
        assertEquals(Set.of("m1.d"), underTest.getRemainingReachableMinions());
    }

    @Test
    void processShouldFinishWhenMinionIsUnreachable() {
        MinionIpAddressesResponse response = new MinionIpAddressesResponse();
        Map<String, JsonNode> responseByMinion = new HashMap<>();
        responseByMinion.put("m1.d", JsonNodeFactory.instance.textNode("false"));
        response.setResult(List.of(responseByMinion));
        when(saltStateService.collectMinionIpAddresses(eq(saltConnector), eq(Optional.of(Set.of("m1.d"))))).thenReturn(response);

        MinionDeletionPoller underTest = new MinionDeletionPoller(saltConnector, Set.of("m1.d"), saltStateService);

        assertEquals(AttemptState.FINISH, underTest.process().getState());
        assertEquals(Set.of(), underTest.getRemainingReachableMinions());
    }
}

