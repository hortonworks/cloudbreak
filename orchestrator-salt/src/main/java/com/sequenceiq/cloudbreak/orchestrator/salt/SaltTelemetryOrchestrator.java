package com.sequenceiq.cloudbreak.orchestrator.salt;

import static com.sequenceiq.common.model.diagnostics.DiagnosticParameters.EXCLUDE_HOSTS_FILTER;
import static com.sequenceiq.common.model.diagnostics.DiagnosticParameters.HOSTS_FILTER;
import static com.sequenceiq.common.model.diagnostics.DiagnosticParameters.HOST_GROUPS_FILTER;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobIdTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltUploadWithPermission;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.ConcurrentParameterizedStateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.ParameterizedStateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.StateAllRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.StateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.Retry;

@Component
public class SaltTelemetryOrchestrator implements TelemetryOrchestrator {

    public static final String MONITORING_INIT = "monitoring.init";

    public static final String FILECOLLECTOR_INIT = "filecollector.init";

    public static final String FILECOLLECTOR_COLLECT = "filecollector.collect";

    public static final String FILECOLLECTOR_UPLOAD = "filecollector.upload";

    public static final String FILECOLLECTOR_CLEANUP = "filecollector.cleanup";

    public static final String NODESTATUS_COLLECT = "nodestatus.collect";

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltTelemetryOrchestrator.class);

    private static final String FILECOLLECTOR_CONFIG_NAME = "filecollector";

    private static final String FLUENT_AGENT_STOP = "fluent.agent-stop";

    private static final String FLUENT_CRONTAB = "fluent.crontab";

    private static final String FLUENT_DOCTOR = "fluent.doctor";

    private static final String TELEMETRY_CLOUD_STORAGE_TEST = "telemetry.test-cloud-storage";

    private static final String READ_WRITE_PERMISSION = "0600";

    private static final String EXECUTE_PERMISSION = "0700";

    private static final String PREFLIGHT_ERROR_START = "PreFlight diagnostics check FAILED";

    private static final String RAM_WARNING = "-- RAM ISSUES ---";

    private static final String ZERO_EXIT_CODE = "Exit code: 0";

    private static final String LOCAL_PREFLIGHT_SCRIPTS_LOCATION = "salt-common/salt/filecollector/scripts/";

    private static final String REMOTE_SCRIPTS_LOCATION = "/opt/salt/scripts";

    private static final String LOCAL_TELEMETRY_SCRIPTS_LOCATION = "salt-common/salt/telemetry/scripts/";

    private static final String TELEMETRY_DEPLOYER_SCRIPT_FILENAME = "cdp_telemetry_deployer.sh";

    private static final String LOCAL_SALT_RESOURCES_LOCATION = "salt";

    private static final String SALT_STATE_UPDATER_SCRIPT = "salt-state-updater.sh";

    private static final String REMOTE_TMP_FOLDER = "/tmp/";

    private static final String[] SCRIPTS_TO_UPLOAD = new String[]{"preflight_check.sh", "filecollector_minion_check.py"};

    private static final String FLUENT_COMPONENT = "fluent";

    private final ExitCriteria exitCriteria;

    private final SaltService saltService;

    private final SaltRunner saltRunner;

    private final int maxTelemetryStopRetry;

    private final int maxNodeStatusCollectRetry;

    private final int maxDiagnosticsCollectionRetry;

    @Value("${cb.max.salt.cloudstorage.validation.retry:3}")
    private int maxCloudStorageValidationRetry;

    @Inject
    private Retry retry;

    public SaltTelemetryOrchestrator(ExitCriteria exitCriteria, SaltService saltService, SaltRunner saltRunner,
            @Value("${cb.max.salt.new.service.telemetry.stop.retry:5}") int maxTelemetryStopRetry,
            @Value("${cb.max.salt.new.service.telemetry.nodestatus.collect.retry:3}") int maxNodeStatusCollectRetry,
            @Value("${cb.max.salt.new.service.diagnostics.collection.retry:360}") int maxDiagnosticsCollectionRetry) {
        this.exitCriteria = exitCriteria;
        this.saltService = saltService;
        this.saltRunner = saltRunner;
        this.maxTelemetryStopRetry = maxTelemetryStopRetry;
        this.maxNodeStatusCollectRetry = maxNodeStatusCollectRetry;
        this.maxDiagnosticsCollectionRetry = maxDiagnosticsCollectionRetry;
    }

    @Override
    public void validateCloudStorage(List<GatewayConfig> allGateways, Set<Node> allNodes, Set<String> targetHostNames,
            Map<String, Object> parameters, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateways);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            runValidation(sc, new ParameterizedStateRunner(targetHostNames, allNodes, TELEMETRY_CLOUD_STORAGE_TEST, parameters), exitCriteriaModel);
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
        OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(sc, baseSaltJobRunner, true);
        Callable<Boolean> saltJobRunBootstrapRunner =
                saltRunner.runner(saltJobIdTracker, exitCriteria, exitCriteriaModel, maxCloudStorageValidationRetry, false);
        saltJobRunBootstrapRunner.call();
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
    public void updateTelemetryComponent(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters, ExitCriteriaModel exitModel,
            String component, String version, boolean skipComponentRestart) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateways);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGateways);
        Set<String> gatewayHostnames = getGatewayHostnames(allGateways);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            uploadScripts(sc, gatewayTargets, exitModel, LOCAL_TELEMETRY_SCRIPTS_LOCATION, TELEMETRY_DEPLOYER_SCRIPT_FILENAME);
            String extraParams = "";
            if (StringUtils.isNotBlank(component)) {
                extraParams += String.format(" -c %s", component);
            }
            if (StringUtils.isNotBlank(version)) {
                extraParams += String.format(" -v %s", version);
            }
            if (skipComponentRestart) {
                extraParams += " -s";
            }
            String upgradeCommand = String.format("%s/%s upgrade%s", REMOTE_SCRIPTS_LOCATION, TELEMETRY_DEPLOYER_SCRIPT_FILENAME, extraParams);
            Target<String> targets = new HostList(gatewayHostnames);
            Map<String, String> upgradeResponse = SaltStates.runCommandOnHosts(retry, sc, targets, upgradeCommand);
            LOGGER.debug("Upgrade response: {}", upgradeResponse);
        } catch (Exception e) {
            LOGGER.info("Error occurred during cdp_telemetry_deployer.sh script upload/execution", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void preFlightDiagnosticsCheck(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateways);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGateways);
        Set<String> gatewayHostnames = getGatewayHostnames(allGateways);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            Target<String> targets = new HostList(gatewayHostnames);
            uploadScripts(sc, gatewayTargets, exitModel, LOCAL_PREFLIGHT_SCRIPTS_LOCATION, SCRIPTS_TO_UPLOAD);
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

    @Override
    public void executeNodeStatusCollection(List<GatewayConfig> allGateways, Set<Node> nodes, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        runSimpleSaltState(allGateways, nodes, exitModel, NODESTATUS_COLLECT, "Error occurred during nodestatus telemetry collect.",
                maxNodeStatusCollectRetry, false);
    }

    @Override
    public Set<Node> collectUnresponsiveNodes(List<GatewayConfig> gatewayConfigs, Set<Node> nodes, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException  {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(gatewayConfigs);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            List<String> missingNodeNames = SaltStates.ping(sc)
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
            Target<String> targets = new HostList(gatewayHostnames);
            uploadScripts(sc, gatewayTargets, exitModel, LOCAL_SALT_RESOURCES_LOCATION, SALT_STATE_UPDATER_SCRIPT);
            String loggingAgentZip = String.format("%s.zip", FLUENT_COMPONENT);
            uploadFileToTargetsWithContentAndPermission(sc, gatewayTargets, exitModel, loggingAgentSaltState,
                    REMOTE_TMP_FOLDER, loggingAgentZip, READ_WRITE_PERMISSION);
            updateLoggingAgentSaltStateDefinition(sc, targets, loggingAgentZip);
            distributeAndExecuteLoggingAgentDoctor(allGateways, nodes, exitModel);
        } catch (Exception e) {
            LOGGER.info("Error occurred during ", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private void runSimpleSaltState(List<GatewayConfig> allGateways, Set<Node> nodes, ExitCriteriaModel exitModel,
            String saltState, String errorMessage, int retryCount, boolean retryOnFail) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateways);
        Set<String> targetHostnames = nodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            StateRunner stateRunner = new StateRunner(targetHostnames, nodes, saltState);
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(sc, stateRunner, retryOnFail);
            Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runner(saltJobIdTracker, exitCriteria, exitModel, retryCount, false);
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
            ConcurrentParameterizedStateRunner stateRunner = new ConcurrentParameterizedStateRunner(targetHostnames, nodes, saltState, parameters);
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(sc, stateRunner, retryOnFail);
            Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runner(saltJobIdTracker, exitCriteria, exitModel, retryCount, false);
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

    private void uploadScripts(SaltConnector saltConnector, Set<String> targets, ExitCriteriaModel exitCriteriaModel,
            String localFolder, String... fileNames)
            throws CloudbreakOrchestratorFailedException {
        for (String fileName : fileNames) {
            uploadFileToTargetsWithPermission(saltConnector, targets, exitCriteriaModel, localFolder, fileName);
        }
    }

    private void uploadFileToTargetsWithPermission(SaltConnector saltConnector, Set<String> targets, ExitCriteriaModel exitCriteriaModel,
            String localFolderPath, String fileName) throws CloudbreakOrchestratorFailedException {
        ClassPathResource scriptResource = new ClassPathResource(Path.of(localFolderPath, fileName).toString(), getClass().getClassLoader());
        byte[] content = asString(scriptResource).getBytes(StandardCharsets.UTF_8);
        uploadFileToTargetsWithContentAndPermission(saltConnector, targets, exitCriteriaModel, content,
                REMOTE_SCRIPTS_LOCATION, fileName, EXECUTE_PERMISSION);
    }

    private void uploadFileToTargetsWithContentAndPermission(SaltConnector saltConnector, Set<String> targets, ExitCriteriaModel exitCriteriaModel,
            byte[] content, String remoteFolder, String fileName, String permission) throws CloudbreakOrchestratorFailedException {
        try {
            OrchestratorBootstrap saltUpload = new SaltUploadWithPermission(saltConnector, targets, remoteFolder,
                    fileName, permission, content);
            Callable<Boolean> saltUploadRunner = saltRunner.runner(saltUpload, exitCriteria, exitCriteriaModel);
            saltUploadRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during file distribute to gateway nodes", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private String asString(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        Map<String, String> preFlightResponses = SaltStates.runCommandOnHosts(retry, sc, targets, command);
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

    private void updateLoggingAgentSaltStateDefinition(SaltConnector sc, Target<String> targets, String loggingAgentZip) {
        String command = String.format("%s/%s -f %s%s -s %s",
                REMOTE_SCRIPTS_LOCATION, SALT_STATE_UPDATER_SCRIPT, REMOTE_TMP_FOLDER, loggingAgentZip, FLUENT_COMPONENT);
        Map<String, String> result = SaltStates.runCommandOnHosts(retry, sc, targets, command);
        LOGGER.debug("Result of partial salt state (fluent) upgrade: {}", result);
    }

    private void distributeAndExecuteLoggingAgentDoctor(List<GatewayConfig> allGateways, Set<Node> nodes, ExitCriteriaModel exitModel) {
        try {
            runSimpleSaltState(allGateways, nodes, exitModel, FLUENT_CRONTAB, "Logging agent crontab distribution operation failed.",
                    1, false);
            runSimpleSaltState(allGateways, nodes, exitModel, FLUENT_DOCTOR, "Logging agent doctor failed.", 1, false);
        } catch (Exception e) {
            LOGGER.warn("Logging agent doctor operation failed. Skipping...", e);
        }
    }
}
