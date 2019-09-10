package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL_ASYNC;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.RUNNER;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.CommandExecutionResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PackageVersionResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PingResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunningJobsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;

@RunWith(MockitoJUnitRunner.class)
public class SaltStatesTest {

    private SaltConnector saltConnector;

    private Target<String> target;

    @Captor
    private ArgumentCaptor<Set<String>> minionIdsCaptor;

    @Before
    public void setUp() {
        Set<String> targets = new HashSet<>();
        targets.add("10-0-0-1.example.com");
        targets.add("10-0-0-2.example.com");
        targets.add("10-0-0-3.example.com");
        target = new HostList(targets);
        saltConnector = mock(SaltConnector.class);
    }

    @Test
    public void addRoleTest() {
        String role = "ambari-server";
        SaltStates.addGrain(saltConnector, target, "roles", role);
        verify(saltConnector, times(1)).run(eq(target), eq("grains.append"), eq(LOCAL), eq(ApplyResponse.class), eq("roles"), eq(role));
    }

    @Test
    public void syncAllTest() {
        SaltStates.syncAll(saltConnector);
        verify(saltConnector, times(1)).run(eq(Glob.ALL), eq("saltutil.sync_all"), eq(LOCAL), eq(ApplyResponse.class));
    }

    @Test
    public void highstateTest() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jobId = "1";
        ApplyResponse response = new ApplyResponse();
        List<Map<String, JsonNode>> result = new ArrayList<>();
        Map<String, JsonNode> resultMap = new HashMap<>();
        resultMap.put("jid", objectMapper.readTree(jobId));
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
        responseStream.close();
        Map<?, ?> responseMap = new ObjectMapper().readValue(response, Map.class);
        when(saltConnector.run(eq("jobs.lookup_jid"), any(), any(), eq("jid"), any())).thenReturn(responseMap);

        Multimap<String, String> jidInfo = SaltStates.jidInfo(saltConnector, jobId, target, StateType.HIGH);
        verify(saltConnector, times(1)).run("jobs.lookup_jid", RUNNER, Map.class, "jid", jobId);

        assertThat(jidInfo.keySet(), hasSize(1));
        assertThat(jidInfo.entries(), hasSize(4));
        String hostName = jidInfo.keySet().iterator().next();
        Collection<String> hostErrors = jidInfo.get(hostName);

        assertThat(hostErrors, containsInAnyOrder(
                "\nName: /opt/ambari-server/ambari-server-init.sh\nComment: Source file salt://ambari/scripts/ambari-server-initttt.sh not found",
                "\nName: ambari-server\nComment: Service ambari-server is already enabled, and is dead",
                "\nComment: Command \"/opt/ambari-server/install-mpack-1.sh\" run\nStderr: + ARGS= + echo yes + ambari-server install-mpack --",
                "\nName: haveged\nComment: Package haveged is already installed."));
    }

    @Test
    public void jidInfoSimpleTest() throws Exception {
        String jobId = "2";

        InputStream responseStream = SaltStatesTest.class.getResourceAsStream("/jid_simple_response.json");
        String response = IOUtils.toString(responseStream);
        Map<?, ?> responseMap = new ObjectMapper().readValue(response, Map.class);

        when(saltConnector.run(eq("jobs.lookup_jid"), any(), any(), eq("jid"), any())).thenReturn(responseMap);

        Multimap<String, String> jidInfo = SaltStates.jidInfo(saltConnector, jobId, target, StateType.SIMPLE);
        verify(saltConnector, times(1)).run("jobs.lookup_jid", RUNNER, Map.class, "jid", jobId);

        assertThat(jidInfo.keySet(), hasSize(1));
        assertThat(jidInfo.entries(), hasSize(3));
        String hostName = jidInfo.keySet().iterator().next();
        Collection<String> hostErrors = jidInfo.get(hostName);

        assertThat(hostErrors, containsInAnyOrder(
                "\nName: /opt/ambari-server/ambari-server-init.sh\nComment: Source file salt://ambari/scripts/ambari-server-initttt.sh not found",
                "\nName: ambari-server\nComment: Service ambari-server is already enabled, and is dead",
                "\nName: haveged\nComment: Package haveged is already installed."));
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
        when(saltConnector.run(eq("jobs.active"), any(), eq(RunningJobsResponse.class))).thenReturn(runningJobsResponse);
        boolean running = SaltStates.jobIsRunning(saltConnector, jid);
        assertTrue(running);

        resultMap.clear();
        running = SaltStates.jobIsRunning(saltConnector, jid);
        assertFalse(running);
    }

    @Test
    public void pingTest() {
        SaltStates.ping(saltConnector, target);
        verify(saltConnector, times(1)).run(any(), eq("test.ping"), eq(LOCAL), eq(PingResponse.class));
    }

    @Test
    public void stopMinionsTest() {
        Map<String, String> privateIpsByFQDN = new HashMap<>();
        privateIpsByFQDN.put("10-0-0-1.example.com", "10.0.0.1");
        privateIpsByFQDN.put("10-0-0-2.example.com", "10.0.0.2");
        privateIpsByFQDN.put("10-0-0-3.example.com", "10.0.0.3");
        SaltStates.stopMinions(saltConnector, privateIpsByFQDN);

        ArgumentCaptor<SaltAction> saltActionArgumentCaptor = ArgumentCaptor.forClass(SaltAction.class);
        verify(saltConnector, times(1)).action(saltActionArgumentCaptor.capture());

        SaltAction saltAction = saltActionArgumentCaptor.getValue();
        assertEquals(SaltActionType.STOP, saltAction.getAction());

        assertThat(saltAction.getMinions(), hasSize(3));

        Set<String> minionAddresses = saltAction.getMinions().stream().map(Minion::getAddress).collect(Collectors.toSet());
        assertThat(minionAddresses, containsInAnyOrder("10.0.0.1", "10.0.0.2", "10.0.0.3"));
    }

    @Test
    public void testGetPackageVersionsWithOnePackage() {
        // GIVEN
        List<Map<String, String>> pkgVersionList = new ArrayList<>();
        Map<String, String> pkgVersionsOnHosts = new HashMap<>();
        pkgVersionsOnHosts.put("host1", "1.0");
        pkgVersionsOnHosts.put("host2", "2.0");
        pkgVersionList.add(pkgVersionsOnHosts);
        PackageVersionResponse resp = new PackageVersionResponse();
        resp.setResult(pkgVersionList);
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package")).thenReturn(resp);
        // WHEN
        Map<String, Map<String, String>> actualResponse = SaltStates.getPackageVersions(saltConnector, "package");
        // THEN
        for (Map.Entry<String, Map<String, String>> e : actualResponse.entrySet()) {
            String expectedVersion = pkgVersionsOnHosts.get(e.getKey());
            Map<String, String> actualPkgVersions = e.getValue();
            assertEquals(1, actualPkgVersions.size());
            assertEquals(expectedVersion, actualPkgVersions.get("package"));
        }
    }

    @Test
    public void testGetPackageVersionWithOnePackageShouldReturnEmptyMapWhenTheListIsEmptyInResponse() {
        // GIVEN
        List<Map<String, String>> pkgVersionsList = new ArrayList<>();
        PackageVersionResponse resp = new PackageVersionResponse();
        resp.setResult(pkgVersionsList);
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package")).thenReturn(resp);
        // WHEN
        Map<String, Map<String, String>> actualResponse = SaltStates.getPackageVersions(saltConnector, "package");
        // THEN
        assertTrue(actualResponse.size() == 0);
    }

    @Test
    public void testGetPackageVersionsWithMorePackages() {
        // GIVEN
        Map<String, Map<String, String>> pkgVersionsOnHosts = new HashMap<>();
        Map<String, String> pkgVersionsOnHost1 = new HashMap<>();
        pkgVersionsOnHost1.put("package1", "1.0");
        pkgVersionsOnHost1.put("package2", "2.0");
        pkgVersionsOnHosts.put("host1", pkgVersionsOnHost1);
        Map<String, String> pkgVersionsOnHost2 = new HashMap<>();
        pkgVersionsOnHost2.put("package1", "2.0");
        pkgVersionsOnHost2.put("package2", "3.0");
        pkgVersionsOnHosts.put("host2", pkgVersionsOnHost2);

        Map<String, String> pkgVersionsOnHost1Resp = new HashMap<>();
        pkgVersionsOnHost1Resp.put("host1", "1.0");
        pkgVersionsOnHost1Resp.put("host2", "2.0");
        Map<String, String> pkgVersionsOnHost2Resp = new HashMap<>();
        pkgVersionsOnHost2Resp.put("host1", "2.0");
        pkgVersionsOnHost2Resp.put("host2", "3.0");
        PackageVersionResponse resp1 = new PackageVersionResponse();
        resp1.setResult(Lists.newArrayList(pkgVersionsOnHost1Resp));
        PackageVersionResponse resp2 = new PackageVersionResponse();
        resp2.setResult(Lists.newArrayList(pkgVersionsOnHost2Resp));
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package1")).thenReturn(resp1);
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package2")).thenReturn(resp2);
        // WHEN
        Map<String, Map<String, String>> actualResponse = SaltStates.getPackageVersions(saltConnector, "package1", "package2");
        // THEN
        Assert.assertEquals(pkgVersionsOnHosts, actualResponse);
    }

    @Test
    public void testGetPackageVersionWithMorePackagesShouldReturnEmptyMapWhenTheListIsEmptyInResponse() {
        // GIVEN
        PackageVersionResponse resp = new PackageVersionResponse();
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package1")).thenReturn(resp);
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package2")).thenReturn(resp);
        // WHEN
        Map<String, Map<String, String>> actualResponse = SaltStates.getPackageVersions(saltConnector, "package1", "package2");
        // THEN
        assertTrue(actualResponse.size() == 0);
    }

    @Test
    public void testGetPackageVersionsThrowsRuntimeException() {
        // GIVEN
        RuntimeException exception = new RuntimeException();
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package1")).thenThrow(exception);
        // WHEN
        try {
            SaltStates.getPackageVersions(saltConnector, "package1", "package2");
        } catch (RuntimeException ex) {
            // THEN
            assertEquals(exception, ex);
        }
    }

    @Test
    public void testRunCommandExecuteCommandSuccesfully() {
        // GIVEN
        Map<String, String> commandOutputs = new HashMap<>();
        commandOutputs.put("host1", "output1");
        commandOutputs.put("host2", "output2");
        CommandExecutionResponse resp = new CommandExecutionResponse();
        List<Map<String, String>> commandOutputsList = new ArrayList<>();
        commandOutputsList.add(commandOutputs);
        resp.setResult(commandOutputsList);
        when(saltConnector.run(Glob.ALL, "cmd.run", LOCAL, CommandExecutionResponse.class, "command")).thenReturn(resp);
        // WHEN
        Map<String, String> actualResult = SaltStates.runCommand(saltConnector, "command");
        // THEN
        assertEquals(commandOutputs, actualResult);
    }

    @Test
    public void testRunCommandShouldReturnEmptyMapWhenTheListIsEmptyInResponse() {
        // GIVEN
        CommandExecutionResponse resp = new CommandExecutionResponse();
        List<Map<String, String>> commandOutputsList = new ArrayList<>();
        resp.setResult(commandOutputsList);
        when(saltConnector.run(Glob.ALL, "cmd.run", LOCAL, CommandExecutionResponse.class, "command")).thenReturn(resp);
        // WHEN
        Map<String, String> actualResult = SaltStates.runCommand(saltConnector, "command");
        // THEN
        assertTrue(actualResult.size() == 0);
    }

    @Test
    public void testRunCommandThrowsRuntimeException() {
        // GIVEN
        RuntimeException exception = new RuntimeException();
        when(saltConnector.run(Glob.ALL, "cmd.run", LOCAL, CommandExecutionResponse.class, "command")).thenThrow(exception);
        // WHEN
        try {
            SaltStates.runCommand(saltConnector, "command");
        } catch (RuntimeException ex) {
            // THEN
            assertEquals(exception, ex);
        }
    }
}
