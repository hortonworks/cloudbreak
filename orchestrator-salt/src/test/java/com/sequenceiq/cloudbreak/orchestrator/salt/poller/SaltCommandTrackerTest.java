package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;

class SaltCommandTrackerTest {

    @Test
    void callHasTargetNodesTest() throws Exception {
        SaltConnector saltConnector = mock(SaltConnector.class);
        SaltJobRunner saltJobRunner = mock(SaltJobRunner.class);
        Set<String> targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");
        when(saltJobRunner.getTargetHostnames()).thenReturn(targets);
        SaltCommandTracker saltCommandTracker = new SaltCommandTracker(saltConnector, saltJobRunner);
        try {
            saltCommandTracker.call();
            fail("shoud throw exception");
        } catch (CloudbreakOrchestratorFailedException e) {
            for (String target : targets) {
                assertTrue(e.getMessage().contains(target));
            }
        }
    }

    @Test
    void callTest() throws Exception {
        SaltConnector saltConnector = mock(SaltConnector.class);
        SaltJobRunner saltJobRunner = mock(SaltJobRunner.class);
        Set<String> targets = new HashSet<>();
        when(saltJobRunner.getTargetHostnames()).thenReturn(targets);
        SaltCommandTracker saltCommandTracker = new SaltCommandTracker(saltConnector, saltJobRunner);
        saltCommandTracker.call();
    }

}