package com.sequenceiq.cloudbreak.orchestrator.salt;

import static com.sequenceiq.common.model.diagnostics.DiagnosticParameters.EXCLUDE_HOSTS_FILTER;
import static com.sequenceiq.common.model.diagnostics.DiagnosticParameters.HOSTS_FILTER;
import static com.sequenceiq.common.model.diagnostics.DiagnosticParameters.HOST_GROUPS_FILTER;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobIdTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.ConcurrentParameterizedStateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.ParameterizedStateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.StateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetrySaltRetryConfig;

@Component
public class SaltTelemetryOrchestrator implements TelemetryOrchestrator {

    public static final String MONITORING_INIT = "monitoring.init";

    public static final String FILECOLLECTOR_INIT = "filecollector.init";

    public static final String FILECOLLECTOR_COLLECT = "filecollector.collect";

    public static final String FILECOLLECTOR_UPLOAD = "filecollector.upload";

    public static final String FILECOLLECTOR_CLEANUP = "filecollector.cleanup";

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltTelemetryOrchestrator.class);

    private static final String FILECOLLECTOR_CONFIG_NAME = "filecollector";

    private static final String FLUENT_AGENT_STOP = "fluent.agent-stop";

    private static final String FLUENT_CRONTAB = "fluent.crontab";

    private static final String FLUENT_DOCTOR = "fluent.doctor";

    private static final String TELEMETRY_CLOUD_STORAGE_TEST = "telemetry.test-cloud-storage";

    private static final String TELEMETRY_UPGRADE = "telemetry.upgrade";

    private static final String PREFLIGHT_ERROR_START = "PreFlight diagnostics check FAILED";

    private static final String RAM_WARNING = "-- RAM ISSUES ---";

    private static final String ZERO_EXIT_CODE = "Exit code: 0";

    private static final String LOCAL_PREFLIGHT_SCRIPTS_LOCATION = "salt-common/salt/filecollector/scripts/";

    private static final String REMOTE_SCRIPTS_LOCATION = "/opt/salt/scripts";

    private static final String LOCAL_TELEMETRY_SCRIPTS_LOCATION = "salt-common/salt/telemetry/scripts/";

    private static final String TELEMETRY_DEPLOYER_SCRIPT_FILENAME = "cdp-telemetry-deployer.sh";

    private static final String[] SCRIPTS_TO_UPLOAD = new String[]{"preflight_check.sh", "filecollector_minion_check.py"};

    private static final String FLUENT_COMPONENT = "fluent";

    private static final int LOGGING_DOCTOR_MAX_RETRY = 3;

    private static final int REINIT_STATE_APPLY_MAX_RETRY = 5;

    @Inject
    private ExitCriteria exitCriteria;

    @Inject
    private SaltService saltService;

    @Inject
    private SaltRunner saltRunner;

    @Inject
    private Retry retry;

    @Inject
    private TelemetrySaltRetryConfig telemetrySaltRetryConfig;

    @Inject
    private SaltStateService saltStateService;

    @Inject
    private SaltPartialStateUpdater saltPartialStateUpdater;

    @Override
    public void validateCloudStorage(List<GatewayConfig> allGateways, Set<Node> allNodes, Set<String> targetHostNames,
            Map<String, Object> parameters, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateways);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            runValidation(sc,
                    new ParameterizedStateRunner(saltStateService, targetHostNames, TELEMETRY_CLOUD_STORAGE_TEST, parameters), exitCriteriaModel);
            LOGGER.debug("Completed validating cloud storage");
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.warn("CloudbreakOrchestratorException occurred during cloud storage validation", e);
            throw e;
        } catch (ExecutionException e) {
            LOGGER.warn("Error occurred during cloud storage validation", e);
            if (e.getCause() instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) e.getCause();
            }
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.warn("Error occurred during cloud storage validation", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private void runValidation(SaltConnector sc, BaseSaltJobRunner baseSaltJobRunner, ExitCriteriaModel exitCriteriaModel) throws Exception {
        OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(saltStateService, sc, baseSaltJobRunner, true);
        Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runnerWithCalculatedErrorCount(saltJobIdTracker, exitCriteria, exitCriteriaModel,
                telemetrySaltRetryConfig.getCloudStorageValidation());
        saltJobRunBootstrapRunner.call();
    }

    @Override
    public void stopTelemetryAgent(List<GatewayConfig> allGateways, Set<Node> nodes, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        runSaltState(allGateways, nodes, Collections.emptyMap(), exitModel, FLUENT_AGENT_STOP,
                "Error occurred during telemetry agent stop.", telemetrySaltRetryConfig.getLoggingAgentStop(), true);
    }

    @Override
    public void initDiagnosticCollection(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        runSaltState(allGateways, nodes, parameters, exitModel, FILECOLLECTOR_INIT,
                "Error occurred during diagnostics filecollector init.", telemetrySaltRetryConfig.getDiagnosticsCollect(), false);
    }

    @Override
    public void updateTelemetryComponent(List<GatewayConfig> allGateways, Set<Node> nodes, ExitCriteriaModel exitModel, Map<String, Object> parameters)
            throws CloudbreakOrchestratorFailedException {
        runSaltState(allGateways, nodes, parameters, exitModel, TELEMETRY_UPGRADE,
                "Error occurred during telemetry upgrade.", telemetrySaltRetryConfig.getTelemetryUpgrade(), false);
    }

    @Override
    public void preFlightDiagnosticsCheck(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateways);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGateways);
        Set<String> gatewayHostnames = getGatewayHostnames(allGateways);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            Target<String> targets = new HostList(gatewayHostnames);
            saltPartialStateUpdater.uploadScripts(sc, gatewayTargets, exitModel, LOCAL_PREFLIGHT_SCRIPTS_LOCATION, SCRIPTS_TO_UPLOAD);
            executeVmPreFlightCheck(sc, targets, nodes, parameters);
        } catch (Exception e) {
            LOGGER.info("Error occurred during preflight_check.sh script upload/execution", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void executeDiagnosticCollection(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        runSaltState(allGateways, nodes, parameters, exitModel, FILECOLLECTOR_COLLECT,
                "Error occurred during diagnostics filecollector collect.", telemetrySaltRetryConfig.getDiagnosticsCollect(), false);
    }

    @Override
    public void uploadCollectedDiagnostics(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        runSaltState(allGateways, nodes, parameters, exitModel, FILECOLLECTOR_UPLOAD,
                "Error occurred during diagnostics filecollector upload.", telemetrySaltRetryConfig.getDiagnosticsCollect(), false);
    }

    @Override
    public void cleanupCollectedDiagnostics(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        runSaltState(allGateways, nodes, parameters, exitModel, FILECOLLECTOR_CLEANUP,
                "Error occurred during diagnostics filecollector cleanup.", telemetrySaltRetryConfig.getDiagnosticsCollect(), false);
    }

    @Override
    public Set<Node> collectUnresponsiveNodes(List<GatewayConfig> gatewayConfigs, Set<Node> nodes, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException  {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(gatewayConfigs);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            List<String> missingNodeNames = saltStateService.ping(sc)
                    .getResultByMinionId().entrySet().stream()
                    .filter(entry -> !entry.getValue())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            return nodes.stream()
                    .filter(n -> missingNodeNames.contains(n.getHostname()))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            LOGGER.info("Cannot collect unresponsive salt minions", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void executeLoggingAgentDiagnostics(byte[] loggingAgentSaltState, List<GatewayConfig> allGateways, Set<Node> nodes, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateways);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGateways);
        Set<String> gatewayHostnames = getGatewayHostnames(allGateways);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            saltPartialStateUpdater.uploadAndUpdateSaltStateComponent(FLUENT_COMPONENT, loggingAgentSaltState, sc, gatewayTargets, gatewayHostnames, exitModel);
            distributeAndExecuteLoggingAgentDoctor(allGateways, nodes, exitModel);
        } catch (Exception e) {
            LOGGER.info("Error occurred during logging agent diagnostics ", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void updateAndRestartTelemetryService(byte[] saltState, String stateName, String applyState, List<GatewayConfig> gatewayConfigs,
            Set<Node> nodes, ExitCriteriaModel exitModel) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(gatewayConfigs);
        Set<String> gatewayTargets = getGatewayPrivateIps(gatewayConfigs);
        Set<String> gatewayHostnames = getGatewayHostnames(gatewayConfigs);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            saltPartialStateUpdater.uploadAndUpdateSaltStateComponent(stateName, saltState, sc, gatewayTargets, gatewayHostnames, exitModel);
            runSimpleSaltState(gatewayConfigs, nodes, exitModel, applyState, String.format("Apply state '%s' failed.", applyState),
                    REINIT_STATE_APPLY_MAX_RETRY, false);
        } catch (Exception e) {
            LOGGER.info("Error occurred during updating salt state definition {} and service state apply: '{}'.", stateName, applyState);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void updatePartialSaltDefinition(byte[] partialSaltState, List<String> components, List<GatewayConfig> gatewayConfigs,
            ExitCriteriaModel exitModel) throws CloudbreakOrchestratorFailedException {
        saltPartialStateUpdater.updatePartialSaltDefinition(partialSaltState, components, gatewayConfigs, exitModel);
    }

    private void runSimpleSaltState(List<GatewayConfig> allGateways, Set<Node> nodes, ExitCriteriaModel exitModel,
            String saltState, String errorMessage, int retryCount, boolean retryOnFail) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateways);
        Set<String> targetHostnames = nodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            StateRunner stateRunner = new StateRunner(saltStateService, targetHostnames, saltState);
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(saltStateService, sc, stateRunner, retryOnFail);
            Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runnerWithCalculatedErrorCount(saltJobIdTracker, exitCriteria, exitModel, retryCount);
            saltJobRunBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.debug(errorMessage, e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    //CHECKSTYLE:OFF
    private void runSaltState(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters,
            ExitCriteriaModel exitModel, String saltState, String errorMessage, int retryCount, boolean retryOnFail)
            throws CloudbreakOrchestratorFailedException {
        //CHECKSTYLE:ON
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateways);
        Set<String> targetHostnames = nodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            ConcurrentParameterizedStateRunner stateRunner =
                    new ConcurrentParameterizedStateRunner(saltStateService, targetHostnames, saltState, parameters);
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(saltStateService, sc, stateRunner, retryOnFail);
            Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runnerWithCalculatedErrorCount(saltJobIdTracker, exitCriteria, exitModel, retryCount);
            saltJobRunBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.debug(errorMessage, e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    private Set<String> getGatewayPrivateIps(Collection<GatewayConfig> allGatewayConfigs) {
        return allGatewayConfigs.stream().filter(GatewayConfig::isPrimary).map(GatewayConfig::getPrivateAddress).collect(Collectors.toSet());
    }

    private Set<String> getGatewayHostnames(Collection<GatewayConfig> allGatewayConfigs) {
        return allGatewayConfigs.stream().filter(GatewayConfig::isPrimary).map(GatewayConfig::getHostname).collect(Collectors.toSet());
    }

    private String getAnyPreflightOutput(Map<String, String> responses) {
        String output = null;
        if (MapUtils.isNotEmpty(responses)) {
            output = responses.entrySet().stream().findAny().map(Map.Entry::getValue).orElse(null);
        }
        return output;
    }

    private boolean isSuccessful(Map<String, String> responses) {
        boolean result = true;
        if (MapUtils.isNotEmpty(responses)) {
            result = responses.entrySet().stream()
                    .anyMatch(entry -> entry.getValue().contains(ZERO_EXIT_CODE));
        }
        return result;
    }

    private String getErrorMessageForPreFlightCheck(Map<String, String> responses) {
        String errorMsg = null;
        if (MapUtils.isNotEmpty(responses)) {
            errorMsg = responses.values().stream()
                    .filter(msg -> msg.contains(PREFLIGHT_ERROR_START))
                    .findFirst()
                    .map(msg -> msg.split(PREFLIGHT_ERROR_START)[1])
                    .orElse(null);
        }
        return errorMsg;
    }

    private void logMemoryWarnings(Map<String, String> responses) {
        if (MapUtils.isNotEmpty(responses)) {
            String fullLogMessage = responses.values().stream()
                    .filter(msg -> msg.contains(RAM_WARNING))
                    .findFirst().orElse(null);
            if (StringUtils.isNotBlank(fullLogMessage)) {
                String[] splitted = fullLogMessage.split(RAM_WARNING);
                if (splitted.length >= 2) {
                    LOGGER.debug("RAM WARNINGS before diagnostics collection: {}", splitted[1]);
                }
            }
        }
    }

    private boolean doesFilecollectorConfigHasHostsFilter(Map<String, Object> parameters) {
        boolean result = false;
        if (MapUtils.isNotEmpty(parameters) && parameters.containsKey(FILECOLLECTOR_CONFIG_NAME)) {
            Map<String, Object> filecollectorMap = (Map<String, Object>) parameters.get(FILECOLLECTOR_CONFIG_NAME);
            if (isAnyFilecollectorConfigKeyNotEmpty(filecollectorMap, HOSTS_FILTER, EXCLUDE_HOSTS_FILTER, HOST_GROUPS_FILTER)) {
                result = true;
            }
        }
        return result;
    }

    private boolean isAnyFilecollectorConfigKeyNotEmpty(Map<String, Object> filecollectorConfig, String... keys) {
        return Arrays.stream(keys)
                .filter(key -> filecollectorConfig.containsKey(key))
                .anyMatch(key -> {
                    Set<String> hostSet = (Set<String>) filecollectorConfig.get(key);
                    return CollectionUtils.isNotEmpty(hostSet);
                });
    }

    private void executeVmPreFlightCheck(SaltConnector sc, Target<String> targets, Set<Node> nodes, Map<String, Object> parameters)
            throws CloudbreakOrchestratorFailedException {
        String command = String.format("%s/%s master-check", REMOTE_SCRIPTS_LOCATION, SCRIPTS_TO_UPLOAD[0]);
        boolean anyHostsFilter = doesFilecollectorConfigHasHostsFilter(parameters);
        if (anyHostsFilter && CollectionUtils.isNotEmpty(nodes)) {
            command += " -h " + nodes.stream().map(Node::getHostname).collect(Collectors.joining(","));
        }
        Map<String, String> preFlightResponses = saltStateService.runCommandOnHosts(retry, sc, targets, command);
        logMemoryWarnings(preFlightResponses);
        boolean successful = isSuccessful(preFlightResponses);
        if (successful) {
            LOGGER.debug("Diagnostics VM preflight check was successful");
        } else {
            String errorMessage = getErrorMessageForPreFlightCheck(preFlightResponses);
            if (StringUtils.isNotBlank(errorMessage)) {
                throw new CloudbreakOrchestratorFailedException(errorMessage);
            } else {
                throw new CloudbreakOrchestratorFailedException(String.format(
                        "Running of preflight_check.sh script got unexpected result: %s", getAnyPreflightOutput(preFlightResponses)));
            }
        }
    }

    private void distributeAndExecuteLoggingAgentDoctor(List<GatewayConfig> allGateways, Set<Node> nodes, ExitCriteriaModel exitModel) {
        try {
            runSimpleSaltState(allGateways, nodes, exitModel, FLUENT_CRONTAB, "Logging agent crontab distribution operation failed.",
                    LOGGING_DOCTOR_MAX_RETRY, false);
            runSimpleSaltState(allGateways, nodes, exitModel, FLUENT_DOCTOR, "Logging agent doctor failed.",
                    LOGGING_DOCTOR_MAX_RETRY, false);
        } catch (Exception e) {
            LOGGER.warn("Logging agent doctor operation failed. Skipping...", e);
        }
    }
}
