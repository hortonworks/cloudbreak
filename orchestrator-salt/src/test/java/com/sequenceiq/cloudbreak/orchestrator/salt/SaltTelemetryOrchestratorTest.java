package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PingResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltFileUpload;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobIdTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltUploadWithPermission;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.ConcurrentParameterizedStateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.StateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetrySaltRetryConfig;

@ExtendWith(MockitoExtension.class)
class SaltTelemetryOrchestratorTest {

    private static final int MAX_DIAGNOSTICS_COLLECTION_RETRY = 360;

    private static final String LOCAL_PREFLIGHT_SCRIPTS_LOCATION = "salt-common/salt/filecollector/scripts/";

    private static final String[] SCRIPTS_TO_UPLOAD = new String[]{"preflight_check.sh", "filecollector_minion_check.py"};

    private final GatewayConfig gatewayConfig = new GatewayConfig("1.1.1.1", "10.0.0.1", "172.16.252.43", "10-0-0-1", 9443,
            "instanceid", "servercert", "clientcert", "clientkey", "saltpasswd", "saltbootpassword",
            "signkey", false, true, "masterPrivateKey", "masterPublicKey", "privatekey", "publickey", null, null);

    private final List<GatewayConfig> gatewayConfigs = List.of(gatewayConfig);

    private final Set<Node> targets = Set.of(
            new Node("10.0.0.1", "1.1.1.1", "10-0-0-1.example.com", "hg"),
            new Node("10.0.0.2", "1.1.1.2", "10-0-0-2.example.com", "hg"),
            new Node("10.0.0.3", "1.1.1.3", "10-0-0-3.example.com", "hg"));

    private final ArgumentCaptor<OrchestratorBootstrap> orchestratorBootstrapArgumentCaptor = ArgumentCaptor.forClass(OrchestratorBootstrap.class);

    private final Callable<Boolean> callable = Mockito.mock(Callable.class);

    private final ExitCriteriaModel exitCriteriaModel = new ExitCriteriaModel() {
    };

    @Mock
    private SaltService saltService;

    @Mock
    private SaltRunner saltRunner;

    @Mock
    private TelemetrySaltRetryConfig telemetrySaltRetryConfig;

    @Mock
    private Retry retry;

    @Mock
    private SaltConnector saltConnector;

    @Mock
    private PingResponse pingResponse;

    @Mock
    private SaltStateService saltStateService;

    @Mock
    private SaltPartialStateUpdater saltPartialStateUpdater;

    @InjectMocks
    private SaltTelemetryOrchestrator underTest;

    @BeforeEach
    void setupTest() throws CloudbreakOrchestratorFailedException {
        lenient().when(telemetrySaltRetryConfig.getDiagnosticsCollect()).thenReturn(MAX_DIAGNOSTICS_COLLECTION_RETRY);
        when(saltService.getPrimaryGatewayConfig(gatewayConfigs)).thenReturn(gatewayConfig);
        when(saltService.createSaltConnector(gatewayConfig)).thenReturn(saltConnector);
        lenient().when(saltRunner.runnerWithCalculatedErrorCount(orchestratorBootstrapArgumentCaptor.capture(), any(), any(), anyInt())).thenReturn(callable);
        lenient().when(saltRunner.runner(orchestratorBootstrapArgumentCaptor.capture(), any(), any())).thenReturn(callable);
        lenient().when(saltRunner.runnerWithConfiguredErrorCount(orchestratorBootstrapArgumentCaptor.capture(), any(), any())).thenReturn(callable);
    }

    @Test
    void testInitDiagnosticCollection() throws Exception {
        Map<String, Object> parameters = getParametersMap();

        underTest.initDiagnosticCollection(gatewayConfigs, targets, parameters, exitCriteriaModel);

        SaltJobIdTracker saltJobIdTracker = (SaltJobIdTracker) orchestratorBootstrapArgumentCaptor.getValue();
        ConcurrentParameterizedStateRunner saltJobRunner = (ConcurrentParameterizedStateRunner) saltJobIdTracker.getSaltJobRunner();

        assertThat(saltJobRunner.getAllNode()).isEmpty();
        assertThat(targets.stream().map(Node::getHostname).collect(Collectors.toSet())).containsExactlyElementsOf(saltJobRunner.getTargetHostnames());
        assertEquals(SaltTelemetryOrchestrator.FILECOLLECTOR_INIT, saltJobRunner.getState());
        verify(callable, times(1)).call();
    }

    @Test
    void testExecuteDiagnosticCollection() throws Exception {
        Map<String, Object> parameters = getParametersMap();

        underTest.executeDiagnosticCollection(gatewayConfigs, targets, parameters, exitCriteriaModel);

        SaltJobIdTracker saltJobIdTracker = (SaltJobIdTracker) orchestratorBootstrapArgumentCaptor.getValue();
        ConcurrentParameterizedStateRunner saltJobRunner = (ConcurrentParameterizedStateRunner) saltJobIdTracker.getSaltJobRunner();

        assertThat(saltJobRunner.getAllNode()).isEmpty();
        assertThat(targets.stream().map(Node::getHostname).collect(Collectors.toSet())).containsExactlyElementsOf(saltJobRunner.getTargetHostnames());
        assertEquals(SaltTelemetryOrchestrator.FILECOLLECTOR_COLLECT, saltJobRunner.getState());
        verify(callable, times(1)).call();
        verify(saltService, times(1)).getPrimaryGatewayConfig(gatewayConfigs);
        verify(saltRunner, times(1)).runnerWithCalculatedErrorCount(any(), any(), any(), anyInt());
        verify(telemetrySaltRetryConfig, times(1)).getDiagnosticsCollect();
    }

    @Test
    void testUploadCollectedDiagnostics() throws Exception {
        Map<String, Object> parameters = getParametersMap();

        underTest.uploadCollectedDiagnostics(gatewayConfigs, targets, parameters, exitCriteriaModel);

        SaltJobIdTracker saltJobIdTracker = (SaltJobIdTracker) orchestratorBootstrapArgumentCaptor.getValue();
        ConcurrentParameterizedStateRunner saltJobRunner = (ConcurrentParameterizedStateRunner) saltJobIdTracker.getSaltJobRunner();

        assertThat(saltJobRunner.getAllNode()).isEmpty();
        assertThat(targets.stream().map(Node::getHostname).collect(Collectors.toSet())).containsExactlyElementsOf(saltJobRunner.getTargetHostnames());
        assertEquals(SaltTelemetryOrchestrator.FILECOLLECTOR_UPLOAD, saltJobRunner.getState());
        verify(callable, times(1)).call();
    }

    @Test
    void testCleanupCollectedDiagnostics() throws Exception {
        Map<String, Object> parameters = getParametersMap();

        underTest.cleanupCollectedDiagnostics(gatewayConfigs, targets, parameters, exitCriteriaModel);

        SaltJobIdTracker saltJobIdTracker = (SaltJobIdTracker) orchestratorBootstrapArgumentCaptor.getValue();
        ConcurrentParameterizedStateRunner saltJobRunner = (ConcurrentParameterizedStateRunner) saltJobIdTracker.getSaltJobRunner();

        assertThat(saltJobRunner.getAllNode()).isEmpty();
        assertThat(targets.stream().map(Node::getHostname).collect(Collectors.toSet())).containsExactlyElementsOf(saltJobRunner.getTargetHostnames());
        assertEquals(SaltTelemetryOrchestrator.FILECOLLECTOR_CLEANUP, saltJobRunner.getState());
        verify(callable, times(1)).call();
    }

    @Test
    void testValidateCloudStorage() throws Exception {
        Set<String> targetHostNames = targets
                .stream()
                .map(Node::getHostname)
                .collect(Collectors.toSet());
        Map<String, Object> parameters = getParametersMap();
        doThrow(new CloudbreakOrchestratorFailedException("Error")).when(callable).call();

        CloudbreakOrchestratorException cloudbreakOrchestratorException = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.validateCloudStorage(gatewayConfigs, targets, targetHostNames, parameters, exitCriteriaModel));

        assertNotNull(cloudbreakOrchestratorException);
        verify(saltService, times(1)).getPrimaryGatewayConfig(gatewayConfigs);
        verify(saltService, times(1)).createSaltConnector(gatewayConfig);
    }

    @Test
    void testStopTelemetryAgent() throws CloudbreakOrchestratorFailedException {
        underTest.stopTelemetryAgent(gatewayConfigs, targets, exitCriteriaModel);

        SaltJobIdTracker saltJobIdTracker = (SaltJobIdTracker) orchestratorBootstrapArgumentCaptor.getValue();
        ConcurrentParameterizedStateRunner saltJobRunner = (ConcurrentParameterizedStateRunner) saltJobIdTracker.getSaltJobRunner();

        assertThat(saltJobRunner.getAllNode()).isEmpty();
        assertThat(targets.stream().map(Node::getHostname).collect(Collectors.toSet())).containsExactlyElementsOf(saltJobRunner.getTargetHostnames());
        assertEquals("fluent.agent-stop", saltJobRunner.getState());
        verify(telemetrySaltRetryConfig, times(1)).getLoggingAgentStop();
        verify(saltService, times(1)).getPrimaryGatewayConfig(gatewayConfigs);
        verify(saltService, times(1)).createSaltConnector(gatewayConfig);
        verify(saltRunner, times(1)).runnerWithCalculatedErrorCount(any(), any(), any(), anyInt());
    }

    @Test
    void testUpdateTelemetryComponent() throws CloudbreakOrchestratorFailedException {
        Map<String, Object> parameters = getParametersMap();

        underTest.updateTelemetryComponent(gatewayConfigs, targets, exitCriteriaModel, parameters);

        SaltJobIdTracker saltJobIdTracker = (SaltJobIdTracker) orchestratorBootstrapArgumentCaptor.getValue();
        ConcurrentParameterizedStateRunner saltJobRunner = (ConcurrentParameterizedStateRunner) saltJobIdTracker.getSaltJobRunner();

        assertThat(saltJobRunner.getAllNode()).isEmpty();
        assertThat(targets.stream().map(Node::getHostname).collect(Collectors.toSet())).containsExactlyElementsOf(saltJobRunner.getTargetHostnames());

        assertEquals("telemetry.upgrade", saltJobRunner.getState());
        verify(telemetrySaltRetryConfig, times(1)).getTelemetryUpgrade();
        verify(saltService, times(1)).getPrimaryGatewayConfig(gatewayConfigs);
        verify(saltService, times(1)).createSaltConnector(gatewayConfig);
        verify(saltRunner, times(1)).runnerWithCalculatedErrorCount(any(), any(), any(), anyInt());
    }

    @Test
    void testCollectUnresponsiveNodes() throws CloudbreakOrchestratorFailedException {
        Node node = new Node("10.0.0.1", "1.1.1.1", "10-0-0-1.example.com", "hg");
        node.setHostname("Test_Unresponsive_Node");
        Set<Node> nodes = new HashSet<>() {{
            add(node);
        }};
        when(pingResponse.getResultByMinionId()).thenReturn(new HashMap<>() {{
            put("Test_Unresponsive_Node", false);
        }});
        when(saltStateService.ping(saltConnector)).thenReturn(pingResponse);

        Set<Node> result = underTest.collectUnresponsiveNodes(gatewayConfigs, nodes, exitCriteriaModel);

        assertEquals(result.stream().findFirst().orElse(null), node);
        verify(saltService, times(1)).getPrimaryGatewayConfig(gatewayConfigs);
        verify(saltService, times(1)).createSaltConnector(gatewayConfig);
    }

    @Test
    void testExecuteLoggingAgentDiagnostics() throws CloudbreakOrchestratorFailedException {
        List<String> saltJobRunnerStates = new ArrayList<>();
        saltJobRunnerStates.add("fluent.crontab");
        saltJobRunnerStates.add("fluent.doctor");

        underTest.executeLoggingAgentDiagnostics(new byte[0], gatewayConfigs, targets, exitCriteriaModel);

        List<OrchestratorBootstrap> orchestratorBootstraps = orchestratorBootstrapArgumentCaptor.getAllValues();

        for (OrchestratorBootstrap ob : orchestratorBootstraps) {
            if (ob instanceof SaltJobIdTracker) {
                StateRunner saltJobRunner = (StateRunner) ((SaltJobIdTracker) ob).getSaltJobRunner();
                assertThat(saltJobRunner.getAllNode()).isEmpty();
                assertThat(targets.stream().map(Node::getHostname).collect(Collectors.toSet())).containsExactlyElementsOf(saltJobRunner.getTargetHostnames());
                assertTrue(saltJobRunnerStates.contains(saltJobRunner.getState()));
            } else if (ob instanceof SaltUploadWithPermission) {
                String saltUploadTarget = ((SaltFileUpload) ob).getTargets().stream().findFirst().orElse(null);
                assertEquals(saltUploadTarget, gatewayConfig.getPrivateAddress());
            }
        }
        verify(saltService, times(3)).getPrimaryGatewayConfig(gatewayConfigs);
        verify(saltService, times(3)).createSaltConnector(gatewayConfig);
        verify(saltRunner, times(2)).runnerWithCalculatedErrorCount(any(), any(), any(), anyInt());
        verify(saltPartialStateUpdater, times(1)).uploadAndUpdateSaltStateComponent(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void testPreFlightDiagnosticsCheck() throws CloudbreakOrchestratorFailedException {
        Map<String, Object> parameters = getParametersMap();
        when(saltStateService.runCommandOnHosts(eq(retry), eq(saltConnector), any(), eq(""))).thenReturn(new HashMap<>() {{
            put("", "");
        }});

        CloudbreakOrchestratorFailedException cloudbreakOrchestratorFailedException = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.preFlightDiagnosticsCheck(gatewayConfigs, targets, parameters, exitCriteriaModel));

        assertNotNull(cloudbreakOrchestratorFailedException);
        verify(saltService, times(1)).getPrimaryGatewayConfig(gatewayConfigs);
        verify(saltService, times(1)).createSaltConnector(gatewayConfig);
        verify(saltPartialStateUpdater, times(1)).uploadScripts(
                any(), any(), any(), eq(LOCAL_PREFLIGHT_SCRIPTS_LOCATION), eq(SCRIPTS_TO_UPLOAD[0]), eq(SCRIPTS_TO_UPLOAD[1]));
    }

    @Test
    void testPreFlightDiagnosticsCheckWithFileCollectorParameter() throws CloudbreakOrchestratorFailedException {
        Map<String, Object> parameters = getParametersMap();
        Map<String, Object> fileCollectorConfig = new HashMap<>() {{
            put("hosts", "hosts");
            put("excludeHosts", "excludeHosts");
            put("hostGroups", "hostGroups");
        }};
        parameters.put("filecollector", fileCollectorConfig);

        CloudbreakOrchestratorFailedException cloudbreakOrchestratorFailedException = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.preFlightDiagnosticsCheck(gatewayConfigs, targets, parameters, exitCriteriaModel));

        assertNotNull(cloudbreakOrchestratorFailedException);
        verify(saltService, times(1)).getPrimaryGatewayConfig(gatewayConfigs);
        verify(saltService, times(1)).createSaltConnector(gatewayConfig);
        verify(saltPartialStateUpdater, times(1)).uploadScripts(
                any(), any(), any(), eq(LOCAL_PREFLIGHT_SCRIPTS_LOCATION), eq(SCRIPTS_TO_UPLOAD[0]), eq(SCRIPTS_TO_UPLOAD[1]));
    }

    private Map<String, Object> getParametersMap() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", "destination");
        parameters.put("issue", "issue");
        parameters.put("description", "description");
        parameters.put("labelFilter", "labelFilter");
        parameters.put("startTime", "startTime");
        parameters.put("endTime", "endTime");
        return parameters;
    }
}
