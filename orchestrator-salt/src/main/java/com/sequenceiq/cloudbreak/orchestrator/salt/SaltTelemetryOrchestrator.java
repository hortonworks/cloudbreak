package com.sequenceiq.cloudbreak.orchestrator.salt;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobIdTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.ConcurrentParameterizedStateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.StateAllRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltRunner;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

@Component
public class SaltTelemetryOrchestrator implements TelemetryOrchestrator {

    public static final String MONITORING_INIT = "monitoring.init";

    public static final String FILECOLLECTOR_INIT = "filecollector.init";

    public static final String FILECOLLECTOR_COLLECT = "filecollector.collect";

    public static final String FILECOLLECTOR_UPLOAD = "filecollector.upload";

    public static final String FILECOLLECTOR_CLEANUP = "filecollector.cleanup";

    private static final String FLUENT_AGENT_STOP = "fluent.agent-stop";

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltTelemetryOrchestrator.class);

    private final ExitCriteria exitCriteria;

    private final SaltService saltService;

    private final SaltRunner saltRunner;

    private final int maxTelemetryStopRetry;

    private final int maxDiagnosticsCollectionRetry;

    public SaltTelemetryOrchestrator(ExitCriteria exitCriteria, SaltService saltService, SaltRunner saltRunner,
            @Value("${cb.max.salt.new.service.telemetry.stop.retry:5}") int maxTelemetryStopRetry,
            @Value("${cb.max.salt.new.service.diagnostics.collection.retry:360}") int maxDiagnosticsCollectionRetry) {
        this.exitCriteria = exitCriteria;
        this.saltService = saltService;
        this.saltRunner = saltRunner;
        this.maxTelemetryStopRetry = maxTelemetryStopRetry;
        this.maxDiagnosticsCollectionRetry = maxDiagnosticsCollectionRetry;
    }

    @Override
    public void installAndStartMonitoring(List<GatewayConfig> allGateways, Set<Node> nodes, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateways);
        Set<String> serverHostname = Sets.newHashSet(primaryGateway.getHostname());
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            StateAllRunner stateAllJobRunner = new StateAllRunner(serverHostname, nodes, MONITORING_INIT);
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(sc, stateAllJobRunner);
            Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runner(saltJobIdTracker, exitCriteria, exitModel);
            saltJobRunBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during cluster monitoring start", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void stopTelemetryAgent(List<GatewayConfig> allGateways, Set<Node> nodes, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        runSaltState(allGateways, nodes, Collections.emptyMap(), exitModel, FLUENT_AGENT_STOP,
                "Error occurred during telemetry agent stop.", maxTelemetryStopRetry, true);
    }

    @Override
    public void initDiagnosticCollection(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        runSaltState(allGateways, nodes, parameters, exitModel, FILECOLLECTOR_INIT,
                "Error occurred during diagnostics filecollector init.", maxDiagnosticsCollectionRetry, false);
    }

    @Override
    public void executeDiagnosticCollection(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        runSaltState(allGateways, nodes, parameters, exitModel, FILECOLLECTOR_COLLECT,
                "Error occurred during diagnostics filecollector collect.", maxDiagnosticsCollectionRetry, false);
    }

    @Override
    public void uploadCollectedDiagnostics(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        runSaltState(allGateways, nodes, parameters, exitModel, FILECOLLECTOR_UPLOAD,
                "Error occurred during diagnostics filecollector upload.", maxDiagnosticsCollectionRetry, false);
    }

    @Override
    public void cleanupCollectedDiagnostics(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        runSaltState(allGateways, nodes, parameters, exitModel, FILECOLLECTOR_CLEANUP,
                "Error occurred during diagnostics filecollector cleanup.", maxDiagnosticsCollectionRetry, false);
    }

    //CHECKSTYLE:OFF
    private void runSaltState(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters,
            ExitCriteriaModel exitModel, String saltState, String errorMessage, int retryCount, boolean retryOnFail)
            throws CloudbreakOrchestratorFailedException {
        //CHECKSTYLE:ON
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateways);
        Set<String> targetHostnames = nodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            ConcurrentParameterizedStateRunner stateRunner = new ConcurrentParameterizedStateRunner(targetHostnames, nodes, saltState, parameters);
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(sc, stateRunner, retryOnFail);
            Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runner(saltJobIdTracker, exitCriteria, exitModel, retryCount, false);
            saltJobRunBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.info(errorMessage, e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }
}
