package com.sequenceiq.cloudbreak.ambari.flow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class AmbariComponentsJoinStatusCheckerTaskTest {

    private final AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

    private final AmbariComponenstJoinStatusCheckerTask underTest = new AmbariComponenstJoinStatusCheckerTask();

    @Test
    public void checkStatusWithNoUnknownComponentState() {
        AmbariHostsCheckerContext ambariHostsCheckerContext = new AmbariHostsCheckerContext(new Stack(), ambariClient, Collections.emptySet(), 0);
        Map<String, Map<String, String>> hostComponentStates = AmbariHostCheckerTestUtils.getHostComponentStates(Arrays.asList(
                AmbariHostCheckerTestUtils.getComponentStates("UNHEALTHY", "HEALTHY"),
                AmbariHostCheckerTestUtils.getComponentStates("HEALTHY", "HEALTHY"),
                AmbariHostCheckerTestUtils.getComponentStates("UNHEALTHY", "UNHEALTHY")
        ));
        when(ambariClient.getHostComponentsStates()).thenReturn(hostComponentStates);
        boolean result = underTest.checkStatus(ambariHostsCheckerContext);
        assertTrue(result);
    }

    @Test
    public void checkStatusWithOneUnknownComponentState() {
        AmbariHostsCheckerContext ambariHostsCheckerContext = new AmbariHostsCheckerContext(new Stack(), ambariClient, Collections.emptySet(), 0);
        Map<String, Map<String, String>> hostComponentStates = AmbariHostCheckerTestUtils.getHostComponentStates(Arrays.asList(
                AmbariHostCheckerTestUtils.getComponentStates("UNHEALTHY", "HEALTHY"),
                AmbariHostCheckerTestUtils.getComponentStates("HEALTHY", "HEALTHY", "UNKNOWN"),
                AmbariHostCheckerTestUtils.getComponentStates("UNHEALTHY", "HEALTHY")
        ));
        when(ambariClient.getHostComponentsStates()).thenReturn(hostComponentStates);
        boolean result = underTest.checkStatus(ambariHostsCheckerContext);
        assertFalse(result);
    }

    @Test
    public void checkStatusWithMultipleUnknownComponentStatesInDifferentHosts() {
        AmbariHostsCheckerContext ambariHostsCheckerContext = new AmbariHostsCheckerContext(new Stack(), ambariClient, Collections.emptySet(), 0);
        Map<String, Map<String, String>> hostComponentStates = AmbariHostCheckerTestUtils.getHostComponentStates(Arrays.asList(
                AmbariHostCheckerTestUtils.getComponentStates("UNKNOWN", "UNKNOWN"),
                AmbariHostCheckerTestUtils.getComponentStates("HEALTHY", "HEALTHY", "UNKNOWN"),
                AmbariHostCheckerTestUtils.getComponentStates("UNHEALTHY", "HEALTHY")
        ));
        when(ambariClient.getHostComponentsStates()).thenReturn(hostComponentStates);
        boolean result = underTest.checkStatus(ambariHostsCheckerContext);
        assertFalse(result);
    }
}