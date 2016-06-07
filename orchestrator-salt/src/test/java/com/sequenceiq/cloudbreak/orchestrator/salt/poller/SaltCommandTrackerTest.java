package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;

public class SaltCommandTrackerTest {

    @Test
    public void callHasTargetNodesTest() throws Exception {
        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);
        SaltJobRunner saltJobRunner = Mockito.mock(SaltJobRunner.class);
        Set<String> targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");
        when(saltJobRunner.getTarget()).thenReturn(targets);
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
    public void callTest() throws Exception {
        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);
        SaltJobRunner saltJobRunner = Mockito.mock(SaltJobRunner.class);
        Set<String> targets = new HashSet<>();
        when(saltJobRunner.getTarget()).thenReturn(targets);
        SaltCommandTracker saltCommandTracker = new SaltCommandTracker(saltConnector, saltJobRunner);
        saltCommandTracker.call();
    }

}