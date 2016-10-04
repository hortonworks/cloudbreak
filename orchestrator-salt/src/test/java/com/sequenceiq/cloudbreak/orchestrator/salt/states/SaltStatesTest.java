package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL_ASYNC;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.RUNNER;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.NetworkInterfaceResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunningJobsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;

@RunWith(MockitoJUnitRunner.class)
public class SaltStatesTest {

    private SaltConnector saltConnector;
    private Target<String> target;

    @Captor
    private ArgumentCaptor<List<String>> minionIdsCaptor;

    @Before
    public void setUp() {
        Set<String> targets = new HashSet<>();
        targets.add("10-0-0-1.example.com");
        targets.add("10-0-0-2.example.com");
        targets.add("10-0-0-3.example.com");
        target = new Compound(targets);
        saltConnector = mock(SaltConnector.class);
    }

    @Test
    public void addRoleTest() {
        String role = "ambari-server";
        SaltStates.addGrain(saltConnector, target, "roles", role);
        verify(saltConnector, times(1)).run(eq(target), eq("grains.append"), eq(LOCAL), eq(ApplyResponse.class), eq("roles"), eq(role));
    }

    @Test
    public void syncGrainsTest() {
        SaltStates.syncGrains(saltConnector);
        verify(saltConnector, times(1)).run(eq(Glob.ALL), eq("saltutil.sync_grains"), eq(LOCAL), eq(ApplyResponse.class));
    }

    @Test
    public void highstateTest() {
        String jobId = "1";
        ApplyResponse response = new ApplyResponse();
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("jid", jobId);
        result.add(resultMap);
        response.setResult(result);
        when(saltConnector.run(any(), eq("state.highstate"), any(), eq(ApplyResponse.class))).thenReturn(response);

        String jid = SaltStates.highstate(saltConnector);
        assertEquals(jobId, jid);
        verify(saltConnector, times(1)).run(eq(Glob.ALL), eq("state.highstate"), eq(LOCAL_ASYNC), eq(ApplyResponse.class));
    }

    @Test
    public void jidInfoHighTest() throws Exception {
        String jobId = "2";

        InputStream responseStream = SaltStatesTest.class.getResourceAsStream("/jid_response.json");
        String response = IOUtils.toString(responseStream);
        Map responseMap = new ObjectMapper().readValue(response, Map.class);

        when(saltConnector.run(any(), eq("jobs.lookup_jid"), any(), any(), eq("jid"), any())).thenReturn(responseMap);

        Multimap<String, String> jidInfo = SaltStates.jidInfo(saltConnector, jobId, target, StateType.HIGH);
        verify(saltConnector, times(1)).run(target, "jobs.lookup_jid", RUNNER, Map.class, "jid", jobId);

        assertThat(jidInfo.keySet(), hasSize(1));
        assertThat(jidInfo.entries(), hasSize(3));
        String hostName = jidInfo.keySet().iterator().next();
        Collection<String> hostErrors = jidInfo.get(hostName);

        assertThat(hostErrors, containsInAnyOrder("Source file salt://ambari/scripts/ambari-server-initttt.sh not found",
                "Service ambari-server is already enabled, and is dead",
                "Package haveged is already installed."));
    }

    @Test
    public void jidInfoSimpleTest() throws Exception {
        String jobId = "2";

        InputStream responseStream = SaltStatesTest.class.getResourceAsStream("/jid_simple_response.json");
        String response = IOUtils.toString(responseStream);
        Map responseMap = new ObjectMapper().readValue(response, Map.class);

        when(saltConnector.run(any(), eq("jobs.lookup_jid"), any(), any(), eq("jid"), any())).thenReturn(responseMap);

        Multimap<String, String> jidInfo = SaltStates.jidInfo(saltConnector, jobId, target, StateType.SIMPLE);
        verify(saltConnector, times(1)).run(target, "jobs.lookup_jid", RUNNER, Map.class, "jid", jobId);

        assertThat(jidInfo.keySet(), hasSize(1));
        assertThat(jidInfo.entries(), hasSize(3));
        String hostName = jidInfo.keySet().iterator().next();
        Collection<String> hostErrors = jidInfo.get(hostName);

        assertThat(hostErrors, containsInAnyOrder("Source file salt://ambari/scripts/ambari-server-initttt.sh not found",
                "Service ambari-server is already enabled, and is dead",
                "Package haveged is already installed."));
    }

    @Test
    public void jobIsRunningTest() {
        String jid = "3";
        RunningJobsResponse runningJobsResponse = new RunningJobsResponse();
        List<Map<String, Map<String, Object>>> result = new ArrayList<>();
        Map<String, Map<String, Object>> resultMap = new HashMap<>();
        resultMap.put(jid, new HashMap<>());
        result.add(resultMap);
        runningJobsResponse.setResult(result);
        when(saltConnector.run(eq(target), eq("jobs.active"), any(), eq(RunningJobsResponse.class), eq("jid"), any())).thenReturn(runningJobsResponse);
        boolean running = SaltStates.jobIsRunning(saltConnector, jid, target);
        assertEquals(true, running);

        resultMap.clear();
        running = SaltStates.jobIsRunning(saltConnector, jid, target);
        assertEquals(false, running);
    }

    @Test
    public void networkInterfaceIPTest() {
        SaltStates.networkInterfaceIP(saltConnector, target);
        verify(saltConnector, times(1)).run(any(), eq("network.interface_ip"), eq(LOCAL), eq(NetworkInterfaceResponse.class), eq("eth0"));
    }

    @Test
    public void removeMinionsTest() {
        List<String> hostNames = new ArrayList<>();
        hostNames.add("10-0-0-1.example.com");
        hostNames.add("10-0-0-2.example.com");
        hostNames.add("10-0-0-3.example.com");
        NetworkInterfaceResponse networkInterfaceResponse = new NetworkInterfaceResponse();
        List<Map<String, String>> result = new ArrayList<>();
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("10-0-0-1.example.com", "10.0.0.1");
        resultMap.put("10-0-0-2.example.com", "10.0.0.2");
        resultMap.put("10-0-0-3.example.com", "10.0.0.3");
        result.add(resultMap);
        networkInterfaceResponse.setResult(result);
        when(saltConnector.run(any(), eq("network.interface_ip"), eq(LOCAL), eq(NetworkInterfaceResponse.class), eq("eth0")))
                .thenReturn(networkInterfaceResponse);
        SaltStates.removeMinions(saltConnector, hostNames);

        ArgumentCaptor<SaltAction> saltActionArgumentCaptor = ArgumentCaptor.forClass(SaltAction.class);
        verify(saltConnector, times(1)).action(saltActionArgumentCaptor.capture());

        SaltAction saltAction = saltActionArgumentCaptor.getValue();
        assertEquals(SaltActionType.STOP, saltAction.getAction());

        assertThat(saltAction.getMinions(), hasSize(3));

        List<String> minionAddressList = saltAction.getMinions().stream().map(Minion::getAddress).collect(Collectors.toList());
        assertThat(minionAddressList, containsInAnyOrder("10.0.0.1", "10.0.0.2", "10.0.0.3"));

        verify(saltConnector, times(1)).wheel(eq("key.delete"), minionIdsCaptor.capture(), any());
        List<String> minionIds = minionIdsCaptor.getValue();
        assertThat(minionIds, containsInAnyOrder("10-0-0-1.example.com", "10-0-0-2.example.com", "10-0-0-3.example.com"));
    }

    @Test
    public void resolveHostNameToMinionHostNameTest() {
        NetworkInterfaceResponse networkInterfaceResponse = new NetworkInterfaceResponse();
        List<Map<String, String>> result = new ArrayList<>();
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("10-0-0-1.example.com", "10.0.0.1");
        resultMap.put("10-0-0-2.example.com", "10.0.0.2");
        resultMap.put("10-0-0-3.example.com", "10.0.0.3");
        result.add(resultMap);
        networkInterfaceResponse.setResult(result);
        when(saltConnector.run(any(), eq("network.interface_ip"), eq(LOCAL), eq(NetworkInterfaceResponse.class), eq("eth0")))
                .thenReturn(networkInterfaceResponse);

        String hostName = SaltStates.resolveHostNameToMinionHostName(saltConnector, "10-0-0-1.example.com");
        assertEquals("10.0.0.1", hostName);
    }

}