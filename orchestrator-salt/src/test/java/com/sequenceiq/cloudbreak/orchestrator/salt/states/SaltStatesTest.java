package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL_ASYNC;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.RUNNER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.common.model.PackageInfo;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyFullResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.CommandExecutionResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.FullNodeResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JidInfoResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PackageVersionResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PingResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunningJobsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltMaster;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.RetryService;

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
        verify(saltConnector, times(1)).run(eq(Glob.ALL), eq("saltutil.sync_all"), eq(LOCAL_ASYNC), eq(ApplyResponse.class));
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
    public void highstateTargetTest() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jobId = "1";
        ApplyResponse response = new ApplyResponse();
        List<Map<String, JsonNode>> result = new ArrayList<>();
        Map<String, JsonNode> resultMap = new HashMap<>();
        resultMap.put("jid", objectMapper.readTree(jobId));
        result.add(resultMap);
        response.setResult(result);
        when(saltConnector.run(any(), eq("state.highstate"), any(), eq(ApplyResponse.class))).thenReturn(response);

        String jid = SaltStates.highstate(saltConnector, target);
        assertEquals(jobId, jid);
        verify(saltConnector, times(1)).run(eq(target), eq("state.highstate"), eq(LOCAL_ASYNC), eq(ApplyResponse.class));
    }

    @Test
    public void jidInfoHighTest() throws Exception {
        String jobId = "2";

        InputStream responseStream = SaltStatesTest.class.getResourceAsStream("/jid_response.json");
        String response = IOUtils.toString(responseStream, Charset.defaultCharset());
        responseStream.close();
        JidInfoResponse jidInfoResponse = new ObjectMapper().readValue(response, JidInfoResponse.class);
        when(saltConnector.run(eq("jobs.lookup_jid"), any(), any(), eq("jid"), any(), any(), any())).thenReturn(jidInfoResponse);

        Multimap<String, Map<String, String>> jidInfo = SaltStates.jidInfo(saltConnector, jobId, StateType.HIGH);
        verify(saltConnector, times(1)).run("jobs.lookup_jid", RUNNER, JidInfoResponse.class, "jid", jobId, "missing", "True");

        assertThat(jidInfo.keySet(), hasSize(1));
        assertThat(jidInfo.entries(), hasSize(4));
        String hostName = jidInfo.keySet().iterator().next();
        Collection<Map<String, String>> hostErrors = jidInfo.get(hostName);

        Map<String, String> expectedMap1 = Map.of(
            "Name", "/opt/ambari-server/ambari-server-init.sh",
            "Comment", "Source file salt://ambari/scripts/ambari-server-initttt.sh not found");
        Map<String, String> expectedMap2 = Map.of(
            "Name", "ambari-server",
            "Comment", "Service ambari-server is already enabled, and is dead");
        Map<String, String> expectedMap3 = Map.of(
            "Comment", "Command \"/opt/ambari-server/install-mpack-1.sh\" run",
            "Stderr", "+ ARGS= + echo yes + ambari-server install-mpack --");
        Map<String, String> expectedMap4 = Map.of(
            "Name", "haveged",
            "Comment", "Package haveged is already installed.");

        assertThat(hostErrors, containsInAnyOrder(expectedMap1, expectedMap2, expectedMap3, expectedMap4));
    }

    @Test
    public void highstateIsRunningTest() throws IOException {
        String runningJid = "20201116101144197633";
        String jobId = "2";

        when(saltConnector.run(eq("jobs.lookup_jid"), any(), any(), eq("jid"), eq("2"), eq("missing"), eq("True"))).thenThrow(
                new SaltExecutionWentWrongException("The function \"state.highstate\" is running as PID 100789 and was started at 2020, " +
                        "Nov 16 10:11:44.197633 with jid " + runningJid));

        InputStream responseStream = SaltStatesTest.class.getResourceAsStream("/jid_response.json");
        String response = IOUtils.toString(responseStream, Charset.defaultCharset());
        responseStream.close();
        JidInfoResponse jidInfoResponse = new ObjectMapper().readValue(response, JidInfoResponse.class);
        when(saltConnector.run(eq("jobs.lookup_jid"), any(), any(), eq("jid"), eq(runningJid), eq("missing"), eq("True"))).thenReturn(jidInfoResponse);

        Multimap<String, Map<String, String>> jidInfo = SaltStates.jidInfo(saltConnector, jobId, StateType.HIGH);
        verify(saltConnector, times(1)).run("jobs.lookup_jid", RUNNER, JidInfoResponse.class, "jid", runningJid, "missing", "True");
        verify(saltConnector, times(1)).run("jobs.lookup_jid", RUNNER, JidInfoResponse.class, "jid", jobId, "missing", "True");

        assertThat(jidInfo.keySet(), hasSize(1));
        assertThat(jidInfo.entries(), hasSize(4));
        String hostName = jidInfo.keySet().iterator().next();
        Collection<Map<String, String>> hostErrors = jidInfo.get(hostName);

        Map<String, String> expectedMap1 = Map.of(
                "Name", "/opt/ambari-server/ambari-server-init.sh",
                "Comment", "Source file salt://ambari/scripts/ambari-server-initttt.sh not found");
        Map<String, String> expectedMap2 = Map.of(
                "Name", "ambari-server",
                "Comment", "Service ambari-server is already enabled, and is dead");
        Map<String, String> expectedMap3 = Map.of(
                "Comment", "Command \"/opt/ambari-server/install-mpack-1.sh\" run",
                "Stderr", "+ ARGS= + echo yes + ambari-server install-mpack --");
        Map<String, String> expectedMap4 = Map.of(
                "Name", "haveged",
                "Comment", "Package haveged is already installed.");

        assertThat(hostErrors, containsInAnyOrder(expectedMap1, expectedMap2, expectedMap3, expectedMap4));
    }

    @Test
    public void highstateIsRunningAndExceptionIsThrownAtTheEndTest() {
        String runningJid = "20201116101144197633";
        String jobId = "2";

        when(saltConnector.run(eq("jobs.lookup_jid"), any(), any(), eq("jid"), eq("2"), eq("missing"), eq("True"))).thenThrow(
                new SaltExecutionWentWrongException("The function \"state.highstate\" is running as PID 100789 and was started at 2020, " +
                        "Nov 16 10:11:44.197633 with jid " + runningJid));

        when(saltConnector.run(eq("jobs.lookup_jid"), any(), any(), eq("jid"), eq("20201116101144197633"), eq("missing"), eq("True"))).thenThrow(
                new SaltExecutionWentWrongException("other error"));

        try {
            SaltStates.jidInfo(saltConnector, jobId, StateType.HIGH);
        } catch (SaltExecutionWentWrongException e) {
            assertEquals("other error", e.getMessage());
        }
        verify(saltConnector, times(1)).run("jobs.lookup_jid", RUNNER, JidInfoResponse.class, "jid", runningJid, "missing", "True");
        verify(saltConnector, times(1)).run("jobs.lookup_jid", RUNNER, JidInfoResponse.class, "jid", jobId, "missing", "True");
    }

    @Test
    public void jidInfoSimpleTest() throws Exception {
        String jobId = "2";

        InputStream responseStream = SaltStatesTest.class.getResourceAsStream("/jid_simple_response.json");
        String response = IOUtils.toString(responseStream);
        JidInfoResponse jidInfoResponse = new ObjectMapper().readValue(response, JidInfoResponse.class);

        when(saltConnector.run(eq("jobs.lookup_jid"), any(), any(), eq("jid"), any())).thenReturn(jidInfoResponse);

        Multimap<String, Map<String, String>> jidInfo = SaltStates.jidInfo(saltConnector, jobId, StateType.SIMPLE);
        verify(saltConnector, times(1)).run("jobs.lookup_jid", RUNNER, JidInfoResponse.class, "jid", jobId);

        assertThat(jidInfo.keySet(), hasSize(1));
        assertThat(jidInfo.entries(), hasSize(3));
        String hostName = jidInfo.keySet().iterator().next();
        Collection<Map<String, String>> hostErrors = jidInfo.get(hostName);

        Map<String, String> expectedMap1 = Map.of(
            "Name", "/opt/ambari-server/ambari-server-init.sh",
            "Comment", "Source file salt://ambari/scripts/ambari-server-initttt.sh not found");
        Map<String, String> expectedMap2 = Map.of(
            "Name", "ambari-server",
            "Comment", "Service ambari-server is already enabled, and is dead");
        Map<String, String> expectedMap3 = Map.of(
            "Name", "haveged",
            "Comment", "Package haveged is already installed.");

        assertThat(hostErrors, containsInAnyOrder(expectedMap1, expectedMap2, expectedMap3));
    }

    @Test
    public void jobIsRunningTest() throws CloudbreakOrchestratorFailedException {
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

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void testJobIsRunningReturnsExceptionOnNullResult() throws CloudbreakOrchestratorFailedException {
        RunningJobsResponse runningJobsResponse = new RunningJobsResponse();
        when(saltConnector.run(eq("jobs.active"), any(), eq(RunningJobsResponse.class))).thenReturn(runningJobsResponse);
        SaltStates.jobIsRunning(saltConnector, "1");
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void testJobIsRunningReturnsExceptionOnNullResponse() throws CloudbreakOrchestratorFailedException {
        when(saltConnector.run(eq("jobs.active"), any(), eq(RunningJobsResponse.class))).thenReturn(null);
        SaltStates.jobIsRunning(saltConnector, "1");
    }

    @Test
    public void pingTest() {
        SaltStates.ping(saltConnector, target);
        verify(saltConnector, times(1)).run(any(), eq("test.ping"), eq(LOCAL), eq(PingResponse.class));
    }

    @Test
    public void stopMinionsTest() {
        Set<String> privateIps = new HashSet<>();
        privateIps.add("10.0.0.1");
        privateIps.add("10.0.0.2");
        privateIps.add("10.0.0.3");
        SaltStates.stopMinions(saltConnector, privateIps);

        ArgumentCaptor<SaltAction> saltActionArgumentCaptor = ArgumentCaptor.forClass(SaltAction.class);
        verify(saltConnector, times(1)).action(saltActionArgumentCaptor.capture());

        SaltAction saltAction = saltActionArgumentCaptor.getValue();
        assertEquals(SaltActionType.STOP, saltAction.getAction());

        assertThat(saltAction.getMinions(), hasSize(3));

        Set<String> minionAddresses = saltAction.getMinions().stream().map(Minion::getAddress).collect(Collectors.toSet());
        assertThat(minionAddresses, containsInAnyOrder("10.0.0.1", "10.0.0.2", "10.0.0.3"));
    }

    @Test
    public void changePasswordTest() throws CloudbreakOrchestratorFailedException {
        String password = "password";
        Set<String> privateIps = new HashSet<>();
        privateIps.add("10.0.0.1");
        privateIps.add("10.0.0.2");
        privateIps.add("10.0.0.3");
        SaltStates.changePassword(saltConnector, privateIps, password);

        ArgumentCaptor<SaltAction> saltActionArgumentCaptor = ArgumentCaptor.forClass(SaltAction.class);
        verify(saltConnector, times(1)).action(saltActionArgumentCaptor.capture());

        SaltAction saltAction = saltActionArgumentCaptor.getValue();
        assertEquals(SaltActionType.CHANGE_PASSWORD, saltAction.getAction());

        assertThat(saltAction.getMasters(), hasSize(3));

        Set<String> minionAddresses = saltAction.getMasters().stream().map(SaltMaster::getAddress).collect(Collectors.toSet());
        assertThat(minionAddresses, containsInAnyOrder("10.0.0.1", "10.0.0.2", "10.0.0.3"));
        assertTrue(saltAction.getMasters().stream().allMatch(master -> master.getAuth().getPassword().equals(password)));
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

        Map<String, Optional<String>> packages = new HashMap<>();
        packages.put("package", Optional.empty());

        // WHEN
        Map<String, List<PackageInfo>> actualResponse = SaltStates.getPackageVersions(saltConnector, packages);
        // THEN
        for (Map.Entry<String, List<PackageInfo>> e : actualResponse.entrySet()) {
            String expectedVersion = pkgVersionsOnHosts.get(e.getKey());
            List<PackageInfo> actualPkgVersions = e.getValue();
            assertEquals(1, actualPkgVersions.size());
            assertEquals(expectedVersion, actualPkgVersions.get(0).getVersion());
        }
    }

    @Test
    public void testGetPackageVersionWithOnePackageShouldReturnEmptyMapWhenTheListIsEmptyInResponse() {
        // GIVEN
        List<Map<String, String>> pkgVersionsList = new ArrayList<>();
        PackageVersionResponse resp = new PackageVersionResponse();
        resp.setResult(pkgVersionsList);
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package")).thenReturn(resp);

        Map<String, Optional<String>> packages = new HashMap<>();
        packages.put("package", Optional.empty());

        // WHEN
        Map<String, List<PackageInfo>> actualResponse = SaltStates.getPackageVersions(saltConnector, packages);
        // THEN
        assertTrue(actualResponse.size() == 0);
    }

    @Test
    public void testGetPackageVersionsWithMorePackages() {
        // GIVEN
        Map<String, List<PackageInfo>> pkgVersionsOnHosts = new HashMap<>();
        List<PackageInfo> pkgVersionsOnHost1 = new ArrayList<>();
        pkgVersionsOnHost1.add(new PackageInfo("package1", "1.0"));
        pkgVersionsOnHost1.add(new PackageInfo("package2", "2.0", "3.0A13466743"));
        pkgVersionsOnHosts.put("host1", pkgVersionsOnHost1);
        List<PackageInfo> pkgVersionsOnHost2 = new ArrayList<>();
        pkgVersionsOnHost2.add(new PackageInfo("package1", "2.0"));
        pkgVersionsOnHost2.add(new PackageInfo("package2", "3.0", "3.0A13466743"));
        pkgVersionsOnHosts.put("host2", pkgVersionsOnHost2);

        Map<String, String> pkgVersionsOnHost1Resp = new HashMap<>();
        pkgVersionsOnHost1Resp.put("host1", "1.0");
        pkgVersionsOnHost1Resp.put("host2", "2.0");
        Map<String, String> pkgVersionsOnHost2Resp = new HashMap<>();
        pkgVersionsOnHost2Resp.put("host1", "2.0-3.0A13466743");
        pkgVersionsOnHost2Resp.put("host2", "3.0-3.0A13466743");
        PackageVersionResponse resp1 = new PackageVersionResponse();
        resp1.setResult(Lists.newArrayList(pkgVersionsOnHost1Resp));
        PackageVersionResponse resp2 = new PackageVersionResponse();
        resp2.setResult(Lists.newArrayList(pkgVersionsOnHost2Resp));
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package1")).thenReturn(resp1);
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package2")).thenReturn(resp2);

        Map<String, Optional<String>> packages = new HashMap<>();
        packages.put("package1", Optional.empty());
        packages.put("package2", Optional.of("(.*)-(.*)"));

        // WHEN
        Map<String, List<PackageInfo>> actualResponse = SaltStates.getPackageVersions(saltConnector, packages);
        // THEN
        Assert.assertEquals(pkgVersionsOnHosts, actualResponse);
    }

    @Test
    public void testGetPackageVersionWithMorePackagesShouldReturnEmptyMapWhenTheListIsEmptyInResponse() {
        // GIVEN
        PackageVersionResponse resp = new PackageVersionResponse();
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package1")).thenReturn(resp);
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package2")).thenReturn(resp);

        Map<String, Optional<String>> packages = new HashMap<>();
        packages.put("package1", Optional.empty());
        packages.put("package2", Optional.of("(.*)-(.*)"));

        // WHEN
        Map<String, List<PackageInfo>> actualResponse = SaltStates.getPackageVersions(saltConnector, packages);
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
            Map<String, Optional<String>> packages = new HashMap<>();
            packages.put("package1", Optional.empty());
            packages.put("package2", Optional.of("(.*)-(.*)"));

            SaltStates.getPackageVersions(saltConnector, packages);
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
        Map<String, String> actualResult = SaltStates.runCommand(spy(RetryService.class), saltConnector, "command");
        // THEN
        assertEquals(commandOutputs, actualResult);
    }

    @Test
    public void testSaltStatesAreMissing() {
        // GIVEN
        ApplyFullResponse applyResponse = new ApplyFullResponse();
        List<Map<String, FullNodeResponse>> result = new ArrayList<>();
        Map<String, FullNodeResponse> nodes = new HashMap<>();
        FullNodeResponse goodResponse = new FullNodeResponse();
        goodResponse.setRetcode(0);
        FullNodeResponse badResponse = new FullNodeResponse();
        badResponse.setRetcode(1);

        nodes.put("10-0-0-1.example.com", goodResponse);
        nodes.put("10-0-0-2.example.com", badResponse);
        result.add(nodes);
        applyResponse.setResult(result);
        when(saltConnector.run(Glob.ALL, "state.show_sls", LOCAL, ApplyFullResponse.class, "STATE-TO-CHECK")).thenReturn(applyResponse);

        // WHEN
        ApplyFullResponse actualResult = SaltStates.showState(saltConnector, "STATE-TO-CHECK");

        // THEN
        assertEquals(applyResponse, actualResult);
    }

    @Test
    public void testRunCommandShouldReturnEmptyMapWhenTheListIsEmptyInResponse() {
        // GIVEN
        CommandExecutionResponse resp = new CommandExecutionResponse();
        List<Map<String, String>> commandOutputsList = new ArrayList<>();
        resp.setResult(commandOutputsList);
        when(saltConnector.run(Glob.ALL, "cmd.run", LOCAL, CommandExecutionResponse.class, "command")).thenReturn(resp);
        // WHEN
        Map<String, String> actualResult = SaltStates.runCommand(spy(RetryService.class), saltConnector, "command");
        // THEN
        assertTrue(actualResult.size() == 0);
    }

    @Test
    public void testRunCommandThrowsRuntimeException() {
        // GIVEN
        RuntimeException exception = new RuntimeException();
        when(saltConnector.run(Glob.ALL, "cmd.run", LOCAL, CommandExecutionResponse.class, "command")).thenThrow(exception);
        // WHEN
        Retry.ActionFailedException actionFailedException = Assertions.assertThrows(Retry.ActionFailedException.class,
                () -> SaltStates.runCommand(spy(RetryService.class), saltConnector, "command"));
        assertEquals("Salt run command failed", actionFailedException.getMessage());
    }

}
