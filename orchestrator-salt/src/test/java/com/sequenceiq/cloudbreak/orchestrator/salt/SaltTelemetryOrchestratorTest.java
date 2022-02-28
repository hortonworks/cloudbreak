package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobIdTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.ConcurrentParameterizedStateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltRunner;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetrySaltRetryConfig;

@ExtendWith(MockitoExtension.class)
class SaltTelemetryOrchestratorTest {

    private static final int MAX_DIAGNOSTICS_COLLECTION_RETRY = 360;

    private final GatewayConfig gatewayConfig = new GatewayConfig("1.1.1.1", "10.0.0.1", "172.16.252.43", "10-0-0-1", 9443,
            "instanceid", "servercert", "clientcert", "clientkey", "saltpasswd", "saltbootpassword",
            "signkey", false, true, "privatekey", "publickey", null, null);

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

    @InjectMocks
    private SaltTelemetryOrchestrator underTest;

    @BeforeEach
    void setupTest() throws CloudbreakOrchestratorFailedException {
        underTest = new SaltTelemetryOrchestrator();
        MockitoAnnotations.openMocks(this);
        when(telemetrySaltRetryConfig.getDiagnosticsCollect()).thenReturn(MAX_DIAGNOSTICS_COLLECTION_RETRY);
        when(saltService.getPrimaryGatewayConfig(gatewayConfigs)).thenReturn(gatewayConfig);
        when(saltService.createSaltConnector(gatewayConfig)).thenReturn(null);
        when(saltRunner.runner(orchestratorBootstrapArgumentCaptor.capture(), any(), any(), anyInt(), anyBoolean()))
                .thenReturn(callable);
    }

    @Test
    void testInitDiagnosticCollection() throws Exception {
        Map<String, Object> parameters = getParametersMap();

        underTest.initDiagnosticCollection(gatewayConfigs, targets, parameters, exitCriteriaModel);

        SaltJobIdTracker saltJobIdTracker = (SaltJobIdTracker) orchestratorBootstrapArgumentCaptor.getValue();
        ConcurrentParameterizedStateRunner saltJobRunner = (ConcurrentParameterizedStateRunner) saltJobIdTracker.getSaltJobRunner();

        Assertions.assertTrue(CollectionUtils.isEqualCollection(targets, saltJobRunner.getAllNode()));
        assertEquals(SaltTelemetryOrchestrator.FILECOLLECTOR_INIT, saltJobRunner.getState());
        verify(callable, Mockito.times(1)).call();
    }

    @Test
    void testExecuteDiagnosticCollection() throws Exception {
        Map<String, Object> parameters = getParametersMap();

        underTest.executeDiagnosticCollection(gatewayConfigs, targets, parameters, exitCriteriaModel);

        SaltJobIdTracker saltJobIdTracker = (SaltJobIdTracker) orchestratorBootstrapArgumentCaptor.getValue();
        ConcurrentParameterizedStateRunner saltJobRunner = (ConcurrentParameterizedStateRunner) saltJobIdTracker.getSaltJobRunner();

        Assertions.assertTrue(CollectionUtils.isEqualCollection(targets, saltJobRunner.getAllNode()));
        assertEquals(SaltTelemetryOrchestrator.FILECOLLECTOR_COLLECT, saltJobRunner.getState());
        verify(callable, Mockito.times(1)).call();
        verify(saltService, Mockito.times(1)).getPrimaryGatewayConfig(gatewayConfigs);
        verify(saltRunner, Mockito.times(1)).runner(any(), any(), any(), anyInt(), anyBoolean());
        verify(telemetrySaltRetryConfig, Mockito.times(1)).getDiagnosticsCollect();
    }

    @Test
    void testUploadCollectedDiagnostics() throws Exception {
        Map<String, Object> parameters = getParametersMap();

        underTest.uploadCollectedDiagnostics(gatewayConfigs, targets, parameters, exitCriteriaModel);

        SaltJobIdTracker saltJobIdTracker = (SaltJobIdTracker) orchestratorBootstrapArgumentCaptor.getValue();
        ConcurrentParameterizedStateRunner saltJobRunner = (ConcurrentParameterizedStateRunner) saltJobIdTracker.getSaltJobRunner();

        Assertions.assertTrue(CollectionUtils.isEqualCollection(targets, saltJobRunner.getAllNode()));
        assertEquals(SaltTelemetryOrchestrator.FILECOLLECTOR_UPLOAD, saltJobRunner.getState());
        verify(callable, Mockito.times(1)).call();
    }

    @Test
    void testCleanupCollectedDiagnostics() throws Exception {
        Map<String, Object> parameters = getParametersMap();

        underTest.cleanupCollectedDiagnostics(gatewayConfigs, targets, parameters, exitCriteriaModel);

        SaltJobIdTracker saltJobIdTracker = (SaltJobIdTracker) orchestratorBootstrapArgumentCaptor.getValue();
        ConcurrentParameterizedStateRunner saltJobRunner = (ConcurrentParameterizedStateRunner) saltJobIdTracker.getSaltJobRunner();

        Assertions.assertTrue(CollectionUtils.isEqualCollection(targets, saltJobRunner.getAllNode()));
        assertEquals(SaltTelemetryOrchestrator.FILECOLLECTOR_CLEANUP, saltJobRunner.getState());
        verify(callable, Mockito.times(1)).call();
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
