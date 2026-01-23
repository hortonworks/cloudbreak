package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL_ASYNC;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.RUNNER;
import static com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService.CMF_JAVA_OPTS_REGEX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.common.model.PackageInfo;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.model.Memory;
import com.sequenceiq.cloudbreak.orchestrator.model.MemoryInfo;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType;
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
import com.sequenceiq.cloudbreak.orchestrator.salt.utils.MinionUtil;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.RetryType;

@ExtendWith(MockitoExtension.class)
class SaltStateServiceTest {

    private static final String VALID_PATERN = "(.*)-([0-9]+)[a-zA-z]*(\\..*)?";

    @Mock
    private SaltConnector saltConnector;

    private Target<String> target;

    @Captor
    private ArgumentCaptor<Set<String>> minionIdsCaptor;

    @Mock
    private MinionUtil minionUtil;

    @Mock
    private Retry retry;

    @InjectMocks
    private SaltStateService underTest;

    @BeforeEach
    void setUp() {
        Set<String> targets = new HashSet<>();
        targets.add("10-0-0-1.example.com");
        targets.add("10-0-0-2.example.com");
        targets.add("10-0-0-3.example.com");
        target = new HostList(targets);
        lenient().when(minionUtil.createMinion(any(), any(), anyBoolean(), anyBoolean())).thenReturn(mock(Minion.class));
        lenient().when(retry.testWithoutRetry(any())).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());
    }

    @Test
    void addRoleTest() {
        String role = "ambari-server";
        underTest.addGrain(saltConnector, target, "roles", role);
        verify(saltConnector, times(1)).run(eq(target), eq("grains.append"), eq(LOCAL), eq(ApplyResponse.class), eq("roles"), eq(role));
    }

    @Test
    void syncAllTest() {
        underTest.syncAll(saltConnector);
        verify(saltConnector, times(1)).run(eq(Glob.ALL), eq("saltutil.sync_all"), eq(LOCAL_ASYNC), eq(ApplyResponse.class));
    }

    @Test
    void highstateTest() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jobId = "1";
        ApplyResponse response = new ApplyResponse();
        List<Map<String, JsonNode>> result = new ArrayList<>();
        Map<String, JsonNode> resultMap = new HashMap<>();
        resultMap.put("jid", objectMapper.readTree(jobId));
        result.add(resultMap);
        response.setResult(result);
        when(saltConnector.run(any(), eq("state.highstate"), any(), eq(ApplyResponse.class))).thenReturn(response);

        ApplyResponse applyResponse = underTest.highstate(saltConnector);
        assertEquals(jobId, applyResponse.getJid());
        verify(saltConnector, times(1)).run(eq(Glob.ALL), eq("state.highstate"), eq(LOCAL_ASYNC), eq(ApplyResponse.class));
    }

    @Test
    void highstateTargetTest() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jobId = "1";
        ApplyResponse response = new ApplyResponse();
        List<Map<String, JsonNode>> result = new ArrayList<>();
        Map<String, JsonNode> resultMap = new HashMap<>();
        resultMap.put("jid", objectMapper.readTree(jobId));
        result.add(resultMap);
        response.setResult(result);
        when(saltConnector.run(any(), eq("state.highstate"), any(), eq(ApplyResponse.class))).thenReturn(response);

        ApplyResponse applyResponse = underTest.highstate(saltConnector, target);
        assertEquals(jobId, applyResponse.getJid());
        verify(saltConnector, times(1)).run(eq(target), eq("state.highstate"), eq(LOCAL_ASYNC), eq(ApplyResponse.class));
    }

    @Test
    void jidInfoHighTest() throws Exception {
        String jobId = "2";

        InputStream responseStream = SaltStateServiceTest.class.getResourceAsStream("/jid_response.json");
        String response = IOUtils.toString(responseStream, Charset.defaultCharset());
        responseStream.close();
        JidInfoResponse jidInfoResponse = new ObjectMapper().readValue(response, JidInfoResponse.class);
        when(saltConnector.run(eq("jobs.lookup_jid"), any(), any(), eq("jid"), any(), any(), any())).thenReturn(jidInfoResponse);

        Multimap<String, Map<String, String>> jidInfo = underTest.jidInfo(saltConnector, jobId, StateType.HIGH);
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
    void highstateIsRunningTest() throws IOException {
        String runningJid = "20201116101144197633";
        String jobId = "2";

        when(saltConnector.run(eq("jobs.lookup_jid"), any(), any(), eq("jid"), eq("2"), eq("missing"), eq("True"))).thenThrow(
                new SaltExecutionWentWrongException("The function \"state.highstate\" is running as PID 100789 and was started at 2020, " +
                        "Nov 16 10:11:44.197633 with jid " + runningJid));

        InputStream responseStream = SaltStateServiceTest.class.getResourceAsStream("/jid_response.json");
        String response = IOUtils.toString(responseStream, Charset.defaultCharset());
        responseStream.close();
        JidInfoResponse jidInfoResponse = new ObjectMapper().readValue(response, JidInfoResponse.class);
        when(saltConnector.run(eq("jobs.lookup_jid"), any(), any(), eq("jid"), eq(runningJid), eq("missing"), eq("True"))).thenReturn(jidInfoResponse);

        Multimap<String, Map<String, String>> jidInfo = underTest.jidInfo(saltConnector, jobId, StateType.HIGH);
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
    void highstateIsRunningAndExceptionIsThrownAtTheEndTest() {
        String runningJid = "20201116101144197633";
        String jobId = "2";

        when(saltConnector.run(eq("jobs.lookup_jid"), any(), any(), eq("jid"), eq("2"), eq("missing"), eq("True"))).thenThrow(
                new SaltExecutionWentWrongException("The function \"state.highstate\" is running as PID 100789 and was started at 2020, " +
                        "Nov 16 10:11:44.197633 with jid " + runningJid));

        when(saltConnector.run(eq("jobs.lookup_jid"), any(), any(), eq("jid"), eq("20201116101144197633"), eq("missing"), eq("True"))).thenThrow(
                new SaltExecutionWentWrongException("other error"));

        try {
            underTest.jidInfo(saltConnector, jobId, StateType.HIGH);
        } catch (SaltExecutionWentWrongException e) {
            assertEquals("other error", e.getMessage());
        }
        verify(saltConnector, times(1)).run("jobs.lookup_jid", RUNNER, JidInfoResponse.class, "jid", runningJid, "missing", "True");
        verify(saltConnector, times(1)).run("jobs.lookup_jid", RUNNER, JidInfoResponse.class, "jid", jobId, "missing", "True");
    }

    @Test
    void jidInfoSimpleTest() throws Exception {
        String jobId = "2";

        InputStream responseStream = SaltStateServiceTest.class.getResourceAsStream("/jid_simple_response.json");
        String response = IOUtils.toString(responseStream);
        JidInfoResponse jidInfoResponse = new ObjectMapper().readValue(response, JidInfoResponse.class);

        when(saltConnector.run(eq("jobs.lookup_jid"), any(), any(), eq("jid"), any())).thenReturn(jidInfoResponse);

        Multimap<String, Map<String, String>> jidInfo = underTest.jidInfo(saltConnector, jobId, StateType.SIMPLE);
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
    void jobIsRunningTest() throws CloudbreakOrchestratorFailedException {
        String jid = "3";
        RunningJobsResponse runningJobsResponse = new RunningJobsResponse();
        List<Map<String, Map<String, Object>>> result = new ArrayList<>();
        Map<String, Map<String, Object>> resultMap = new HashMap<>();
        resultMap.put(jid, new HashMap<>());
        result.add(resultMap);
        runningJobsResponse.setResult(result);
        when(saltConnector.run(eq("jobs.active"), any(), eq(RunningJobsResponse.class))).thenReturn(runningJobsResponse);
        boolean running = underTest.jobIsRunning(saltConnector, jid);
        assertTrue(running);

        resultMap.clear();
        running = underTest.jobIsRunning(saltConnector, jid);
        assertFalse(running);
    }

    @Test
    void testJobIsRunningReturnsExceptionOnNullResult() throws CloudbreakOrchestratorFailedException {
        RunningJobsResponse runningJobsResponse = new RunningJobsResponse();
        when(saltConnector.run(eq("jobs.active"), any(), eq(RunningJobsResponse.class))).thenReturn(runningJobsResponse);
        Assertions.assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.jobIsRunning(saltConnector, "1"));
    }

    @Test
    void testJobIsRunningReturnsExceptionOnNullResponse() throws CloudbreakOrchestratorFailedException {
        when(saltConnector.run(eq("jobs.active"), any(), eq(RunningJobsResponse.class))).thenReturn(null);
        Assertions.assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.jobIsRunning(saltConnector, "1"));
    }

    @Test
    void pingTest() {
        underTest.ping(saltConnector, target);
        verify(saltConnector, times(1)).run(any(), eq("test.ping"), eq(LOCAL), eq(PingResponse.class));
    }

    @Test
    void stopMinionsTest() {
        Set<String> privateIps = new HashSet<>();
        privateIps.add("10.0.0.1");
        privateIps.add("10.0.0.2");
        privateIps.add("10.0.0.3");
        underTest.stopMinions(saltConnector, privateIps);

        ArgumentCaptor<SaltAction> saltActionArgumentCaptor = ArgumentCaptor.forClass(SaltAction.class);
        verify(saltConnector, times(1)).action(saltActionArgumentCaptor.capture());

        SaltAction saltAction = saltActionArgumentCaptor.getValue();
        assertEquals(SaltActionType.STOP, saltAction.getAction());

        assertThat(saltAction.getMinions(), hasSize(3));

        Set<String> minionAddresses = saltAction.getMinions().stream().map(Minion::getAddress).collect(Collectors.toSet());
        assertThat(minionAddresses, containsInAnyOrder("10.0.0.1", "10.0.0.2", "10.0.0.3"));
    }

    @Test
    void changePasswordTest() throws CloudbreakOrchestratorFailedException {
        String password = "password";
        Set<String> privateIps = new HashSet<>();
        privateIps.add("10.0.0.1");
        privateIps.add("10.0.0.2");
        privateIps.add("10.0.0.3");
        underTest.changePassword(saltConnector, privateIps, password);

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
    void bootstrapTest() {
        Set<Node> targets = new HashSet<>();
        targets.add(new Node("10.0.0.1", null, null, "hg"));
        targets.add(new Node("10.0.0.2", null, null, "hg"));
        targets.add(new Node("10.0.0.3", null, null, "hg"));

        GenericResponses genericResponses = mock(GenericResponses.class);
        when(saltConnector.action(any())).thenReturn(genericResponses);

        BootstrapParams params = new BootstrapParams();
        params.setRestartNeeded(true);
        params.setRestartNeededFlagSupported(false);

        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        String gatewayIp = "8.8.8.8";
        when(gatewayConfig.getPrivateAddress()).thenReturn(gatewayIp);

        GenericResponses result = underTest.bootstrap(saltConnector, params, List.of(gatewayConfig), targets);

        assertEquals(genericResponses, result);
        targets.forEach(node -> verify(minionUtil).createMinion(node, List.of(gatewayIp), params.isSaltBootstrapFpSupported(), params.isRestartNeeded()));
        ArgumentCaptor<SaltAction> captor = ArgumentCaptor.forClass(SaltAction.class);
        verify(saltConnector).action(captor.capture());
        SaltAction saltAction = captor.getValue();
        List<Minion> minions = saltAction.getMinions();
        assertEquals(3, minions.size());
    }

    @Test
    void testGetPackageVersionsWithOnePackage() {
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
        Map<String, List<PackageInfo>> actualResponse = underTest.getPackageVersions(saltConnector, packages);
        // THEN
        for (Map.Entry<String, List<PackageInfo>> e : actualResponse.entrySet()) {
            String expectedVersion = pkgVersionsOnHosts.get(e.getKey());
            List<PackageInfo> actualPkgVersions = e.getValue();
            assertEquals(1, actualPkgVersions.size());
            assertEquals(expectedVersion, actualPkgVersions.get(0).getVersion());
        }
    }

    @Test
    void testGetPackageVersionWithOnePackageShouldReturnEmptyMapWhenTheListIsEmptyInResponse() {
        // GIVEN
        List<Map<String, String>> pkgVersionsList = new ArrayList<>();
        PackageVersionResponse resp = new PackageVersionResponse();
        resp.setResult(pkgVersionsList);
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package")).thenReturn(resp);

        Map<String, Optional<String>> packages = new HashMap<>();
        packages.put("package", Optional.empty());

        // WHEN
        Map<String, List<PackageInfo>> actualResponse = underTest.getPackageVersions(saltConnector, packages);
        // THEN
        assertEquals(0, actualResponse.size());
    }

    public static Stream<Arguments> packageVersionPatters() {
        return Stream.of(
                //TestCase, ActualVersion, Pattern, Version, BuildNumber
                Arguments.of("NORMAL_VERSION", "7.5.2-27070447.el7", "(.*)-([0-9]+)[a-zA-z]*(\\..*)?", "7.5.2", "27070447"),
                Arguments.of("PATCHED_VERSION", "7.5.2-27070447p.el7", "(.*)-([0-9]+)[a-zA-z]*(\\..*)?", "7.5.2", "27070447"),
                Arguments.of("WILD_VERSION", "7.5.2-27070447psdfsdfsdfsdg.el7", "(.*)-([0-9]+)[a-zA-z]*(\\..*)?", "7.5.2", "27070447"),
                Arguments.of("NO_PATTERN", "7.5.2-27070447p.el7", null, "7.5.2-27070447p.el7", null),
                Arguments.of("NORMAL_VERSION_NO_OS", "7.5.2-27070447", "(.*)-([0-9]+)[a-zA-z]*(\\..*)?", "7.5.2", "27070447"),
                Arguments.of("PATCHED_VERSION_NO_OS", "7.5.2-27070447p", "(.*)-([0-9]+)[a-zA-z]*(\\..*)?", "7.5.2", "27070447"),
                Arguments.of("WILD_VERSION_NO_OS", "7.5.2-27070447psdfsdfsdfsdg", "(.*)-([0-9]+)[a-zA-z]*(\\..*)?", "7.5.2", "27070447"),
                Arguments.of("NO_PATTERN_NO_OS", "7.5.2-27070447p", null, "7.5.2-27070447p", null)
        );
    }

    @ParameterizedTest(name = "{0}: with actual version {1}, with pattern {2} with version {3}, with build number {4}")
    @MethodSource("packageVersionPatters")
    void testGetPackageVersionsWithMorePackages(String testCase, String actualVersion, String pattern, String version, String buildNumber) {

        // GIVEN
        Map<String, List<PackageInfo>> pkgVersionsOnHosts = new HashMap<>();
        List<PackageInfo> pkgVersionsOnHost1 = new ArrayList<>();
        pkgVersionsOnHost1.add(new PackageInfo("package2", version, buildNumber));
        pkgVersionsOnHosts.put("host1", pkgVersionsOnHost1);

        Map<String, String> pkgVersionsOnHost1Resp = new HashMap<>();
        pkgVersionsOnHost1Resp.put("host1", actualVersion);
        PackageVersionResponse resp1 = new PackageVersionResponse();
        resp1.setResult(Lists.newArrayList(pkgVersionsOnHost1Resp));
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package2")).thenReturn(resp1);

        Map<String, Optional<String>> packages = new HashMap<>();
        packages.put("package2", Objects.isNull(pattern) ? Optional.empty() : Optional.of(pattern));

        // WHEN
        Map<String, List<PackageInfo>> actualResponse = underTest.getPackageVersions(saltConnector, packages);
        // THEN
        assertEquals(pkgVersionsOnHosts, actualResponse);
    }

    @Test
    void testGetPackageVersionsWithMorePackages() {
        // GIVEN
        Map<String, List<PackageInfo>> pkgVersionsOnHosts = new HashMap<>();
        List<PackageInfo> pkgVersionsOnHost1 = new ArrayList<>();
        pkgVersionsOnHost1.add(new PackageInfo("package1", "1.0"));
        pkgVersionsOnHost1.add(new PackageInfo("package2", "2.0", "3"));
        pkgVersionsOnHosts.put("host1", pkgVersionsOnHost1);
        List<PackageInfo> pkgVersionsOnHost2 = new ArrayList<>();
        pkgVersionsOnHost2.add(new PackageInfo("package1", "2.0"));
        pkgVersionsOnHost2.add(new PackageInfo("package2", "7.5.2", "27070447"));
        pkgVersionsOnHosts.put("host2", pkgVersionsOnHost2);

        Map<String, String> pkgVersionsOnHost1Resp = new HashMap<>();
        pkgVersionsOnHost1Resp.put("host1", "1.0");
        pkgVersionsOnHost1Resp.put("host2", "2.0");
        Map<String, String> pkgVersionsOnHost2Resp = new HashMap<>();
        pkgVersionsOnHost2Resp.put("host1", "2.0-3.0A13466743");
        pkgVersionsOnHost2Resp.put("host2", "7.5.2-27070447p");
        PackageVersionResponse resp1 = new PackageVersionResponse();
        resp1.setResult(Lists.newArrayList(pkgVersionsOnHost1Resp));
        PackageVersionResponse resp2 = new PackageVersionResponse();
        resp2.setResult(Lists.newArrayList(pkgVersionsOnHost2Resp));
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package1")).thenReturn(resp1);
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package2")).thenReturn(resp2);

        Map<String, Optional<String>> packages = new HashMap<>();
        packages.put("package1", Optional.empty());
        packages.put("package2", Optional.of(VALID_PATERN));

        // WHEN
        Map<String, List<PackageInfo>> actualResponse = underTest.getPackageVersions(saltConnector, packages);
        // THEN
        assertEquals(pkgVersionsOnHosts, actualResponse);
    }

    @Test
    void testGetPackageVersionWithMorePackagesShouldReturnEmptyMapWhenTheListIsEmptyInResponse() {
        // GIVEN
        PackageVersionResponse resp = new PackageVersionResponse();
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package1")).thenReturn(resp);
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package2")).thenReturn(resp);

        Map<String, Optional<String>> packages = new HashMap<>();
        packages.put("package1", Optional.empty());
        packages.put("package2", Optional.of(VALID_PATERN));

        // WHEN
        Map<String, List<PackageInfo>> actualResponse = underTest.getPackageVersions(saltConnector, packages);
        // THEN
        assertEquals(0, actualResponse.size());
    }

    @Test
    void testGetPackageVersionsThrowsRuntimeException() {
        // GIVEN
        RuntimeException exception = new RuntimeException();
        when(saltConnector.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, "package1")).thenThrow(exception);
        // WHEN
        try {
            Map<String, Optional<String>> packages = new HashMap<>();
            packages.put("package1", Optional.empty());
            packages.put("package2", Optional.of(VALID_PATERN));

            underTest.getPackageVersions(saltConnector, packages);
        } catch (RuntimeException ex) {
            // THEN
            assertEquals(exception, ex);
        }
    }

    @Test
    void testRunCommandExecuteCommandSuccesfully() {
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
        Map<String, String> actualResult = underTest.runCommand(saltConnector, "command", RetryType.NO_RETRY);
        // THEN
        assertEquals(commandOutputs, actualResult);
    }

    @Test
    void testSaltStatesAreMissing() {
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
        ApplyFullResponse actualResult = underTest.showState(saltConnector, "STATE-TO-CHECK");

        // THEN
        assertEquals(applyResponse, actualResult);
    }

    @Test
    void testRunCommandShouldReturnEmptyMapWhenTheListIsEmptyInResponse() {
        // GIVEN
        CommandExecutionResponse resp = new CommandExecutionResponse();
        List<Map<String, String>> commandOutputsList = new ArrayList<>();
        resp.setResult(commandOutputsList);
        when(saltConnector.run(Glob.ALL, "cmd.run", LOCAL, CommandExecutionResponse.class, "command")).thenReturn(resp);
        // WHEN
        Map<String, String> actualResult = underTest.runCommand(saltConnector, "command", RetryType.NO_RETRY);
        // THEN
        assertEquals(0, actualResult.size());
    }

    @Test
    void testRunCommandThrowsRuntimeException() {
        // GIVEN
        RuntimeException exception = new RuntimeException();
        when(saltConnector.run(Glob.ALL, "cmd.run", LOCAL, CommandExecutionResponse.class, "command")).thenThrow(exception);
        // WHEN
        Retry.ActionFailedException actionFailedException = assertThrows(Retry.ActionFailedException.class,
                () -> underTest.runCommand(saltConnector, "command", RetryType.NO_RETRY));
        assertEquals("Salt run command failed", actionFailedException.getMessage());
    }

    @Test
    void testSaltJobsLookupJidReturnsEmptyResponseWithHighState() {
        String jobId = "2";
        JidInfoResponse emptyJidInfo = new JidInfoResponse();
        when(saltConnector.run(eq("jobs.lookup_jid"), any(), any(), eq("jid"), any(), any(), any())).thenReturn(emptyJidInfo);
        SaltEmptyResponseException saltEmptyResponseException = Assertions.assertThrows(
                SaltEmptyResponseException.class, () -> underTest.jidInfo(saltConnector, jobId, StateType.HIGH));
        assertTrue(saltEmptyResponseException.getMessage().contains("jobs.lookup_jid returns an empty response"));
    }

    @Test
    void testSaltJobsLookupJidReturnsEmptyResponseWithSimpleState() {
        String jobId = "2";
        JidInfoResponse emptyJidInfo = new JidInfoResponse();
        when(saltConnector.run(eq("jobs.lookup_jid"), any(), any(), eq("jid"), any())).thenReturn(emptyJidInfo);
        SaltEmptyResponseException saltEmptyResponseException = Assertions.assertThrows(
                SaltEmptyResponseException.class, () -> underTest.jidInfo(saltConnector, jobId, StateType.SIMPLE));
        assertTrue(saltEmptyResponseException.getMessage().contains("jobs.lookup_jid returns an empty response"));
    }

    void testSetClouderaManagerMemory() {
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfig.getHostname()).thenReturn("host.master0.site");

        underTest.setClouderaManagerMemory(saltConnector, gatewayConfig, Memory.of(4, "GB"));

        ArgumentCaptor<Target<String>> captor = ArgumentCaptor.forClass(Target.class);
        verify(saltConnector).run(
                captor.capture(),
                eq("file.replace"),
                eq(LOCAL),
                eq(Map.class),
                eq("/etc/default/cloudera-scm-server"),
                eq("Xmx\\d+G"),
                eq("Xmx4G"));

        assertEquals("host.master0.site", captor.getValue().getTarget());
    }

    @Test
    void testGetConfiguredClouderaManagerMemoryWithSingleDigit() {
        testGetClouderaManagerMemoryWhenConfigIs(
                "export CMF_JAVA_OPTS=\"-Xmx4G -XX:MaxPermSize=256m -XX:+HeapDumpOnOutOfMemoryError " +
                        "-XX:HeapDumpPath=/tmp -Dcom.sun.management.jmxremote.ssl.enabled.protocols=TLSv1.2\"",
                Optional.of(Memory.ofGigaBytes(4)));
    }

    @Test
    void testGetConfiguredClouderaManagerMemoryWithMultipleDigit() {
        testGetClouderaManagerMemoryWhenConfigIs(
                "export CMF_JAVA_OPTS=\"-Xmx12G -XX:MaxPermSize=256m -XX:+HeapDumpOnOutOfMemoryError " +
                        "-XX:HeapDumpPath=/tmp -Dcom.sun.management.jmxremote.ssl.enabled.protocols=TLSv1.2\"",
                Optional.of(Memory.ofGigaBytes(12)));
    }

    @Test
    void testGetConfiguredClouderaManagerMemoryWhenConfigDoesntContainJvmMemorySetting() {
        testGetClouderaManagerMemoryWhenConfigIs(
                "export CMF_JAVA_OPTS=\"-XX:MaxPermSize=256m -XX:+HeapDumpOnOutOfMemoryError " +
                        "-XX:HeapDumpPath=/tmp -Dcom.sun.management.jmxremote.ssl.enabled.protocols=TLSv1.2\"",
                Optional.empty());
    }

    @Test
    void testGetVmMemoryInfo() {
        Map<String, Map<String, String>> memoryResult = Map.of("MemTotal", Map.of("value", "1", "unit", "kB"));
        when(saltConnector.run(any(Target.class), any(String.class), any(SaltClientType.class), any()))
                .thenReturn(Map.of("return", List.of(Map.of("host.master0.site", memoryResult))));

        Optional<MemoryInfo> memoryInfo = underTest.getMemoryInfo(saltConnector, "host.master0.site");

        assertNotNull(memoryInfo);
        assertTrue(memoryInfo.isPresent());
        assertEquals(Memory.of(1, "kb"), memoryInfo.get().getTotalMemory());
    }

    @Test
    void testCmfJavaOptsRegex() {
        Pattern pattern = Pattern.compile(CMF_JAVA_OPTS_REGEX);

        String originalCmfJavaOptsWithoutTimeout = "export CMF_JAVA_OPTS=\"-Xmx8G -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp " +
                "-Dcom.sun.management.jmxremote.ssl.enabled.protocols=TLSv1.2\"";
        if (!pattern.matcher(originalCmfJavaOptsWithoutTimeout).matches()) {
            fail("Regex should match");
        }

        String cmfJavaOptsWithTimeout = "export CMF_JAVA_OPTS=\"-Xmx8G -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp " +
                "-Dcom.sun.management.jmxremote.ssl.enabled.protocols=TLSv1.2 " +
                "-Dcom.cloudera.cmf.service.AbstractCommandHandler.WORKAROUND_TIMEOUT_INTERVAL=600\"";
        if (pattern.matcher(cmfJavaOptsWithTimeout).matches()) {
            fail("Regex should not match");
        }
    }

    void testGetClouderaManagerMemoryWhenConfigIs(String cmConfig, Optional<Memory> expectedMemory) {
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfig.getHostname()).thenReturn("host.master0.site");
        when(saltConnector.run(any(Target.class), any(String.class), any(SaltClientType.class), any(), any(String.class)))
                .thenReturn(Map.of("return", List.of(Map.of("host.master0.site", cmConfig))));

        Optional<Memory> clouderaManagerMemory = underTest.getClouderaManagerMemory(saltConnector, gatewayConfig);

        assertEquals(expectedMemory, clouderaManagerMemory);
        ArgumentCaptor<Target<String>> captor = ArgumentCaptor.forClass(Target.class);
        verify(saltConnector).run(
                captor.capture(),
                eq("cmd.run"),
                eq(LOCAL),
                eq(Map.class),
                eq("cat /etc/default/cloudera-scm-server | grep '^export CMF_JAVA_OPTS' | tail -n1"));
        assertEquals("host.master0.site", captor.getValue().getTarget());
    }
}