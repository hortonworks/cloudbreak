package com.sequenceiq.cloudbreak.orchestrator.salt;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SALT;
import static com.sequenceiq.cloudbreak.common.type.RecipeExecutionPhase.PRE_CLOUDERA_MANAGER_START;
import static com.sequenceiq.cloudbreak.common.type.RecipeExecutionPhase.PRE_SERVICE_DEPLOYMENT;
import static com.sequenceiq.cloudbreak.common.type.RecipeExecutionPhase.convert;
import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspath;
import static java.util.function.Predicate.not;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.model.PackageInfo;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.orchestration.NodeVolumes;
import com.sequenceiq.cloudbreak.common.orchestration.OrchestratorAware;
import com.sequenceiq.cloudbreak.common.type.RecipeExecutionPhase;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorTimeoutException;
import com.sequenceiq.cloudbreak.orchestrator.host.GrainOperation;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorGrainRunnerParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.CmAgentStopFlags;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.model.KeytabModel;
import com.sequenceiq.cloudbreak.orchestrator.model.Memory;
import com.sequenceiq.cloudbreak.orchestrator.model.MemoryInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostAndRoleTarget;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusSaltResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.grain.GrainUploader;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.PillarSave;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltBootstrapFactory;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobIdTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltUpload;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltUploadWithPermission;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.ConcurrentParameterizedStateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainAddRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainRemoveRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.HighStateAllRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.HighStateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.MineUpdateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.ModifyGrainBase;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.ParameterizedStateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.StateAllRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.StateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.SyncAllRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltCommandRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;
import com.sequenceiq.cloudbreak.orchestrator.salt.utils.GrainsJsonPropertyUtil;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.util.CompressUtil;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class SaltOrchestrator implements HostOrchestrator {

    private static final int SLEEP_TIME = 10000;

    private static final int SLEEP_TIME_IN_SEC = SLEEP_TIME / 1000;

    private static final int SECONDS_IN_MIN = 60;

    private static final int DATABASE_DR_EACH_RETRY_IN_SEC = 10;

    private static final String FREEIPA_MASTER_ROLE = "freeipa_primary";

    private static final String FREEIPA_MASTER_REPLACEMENT_ROLE = "freeipa_primary_replacement";

    private static final String FREEIPA_REPLICA_ROLE = "freeipa_replica";

    private static final String DATABASE_BACKUP = "postgresql.disaster_recovery.backup";

    private static final String DATABASE_RESTORE = "postgresql.disaster_recovery.restore";

    private static final String CM_SERVER_RESTART = "cloudera.manager.restart";

    private static final String CM_AGENT_CERTDIR_PERMISSION = "cloudera.agent.agent-cert-permission";

    private static final String CREATE_USER_HOME_CRON = "cloudera.createuserhome";

    private static final String DISK_INITIALIZE = "format-and-mount-initialize.sh";

    private static final String DISK_COMMON = "format-and-mount-common.sh";

    private static final String DISK_FORMAT = "find-device-and-format.sh";

    private static final String DISK_MOUNT = "mount-disks.sh";

    private static final String DISK_SCRIPT_PATH = "salt/bootstrapnodes/";

    private static final String SRV_SALT_DISK = "/srv/salt/disk";

    private static final String PERMISSION = "0600";

    private static final DateTimeFormatter CHAGE_DATE_PATTERN = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private static final String CHAGE_DATE_NEVER = "never";

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltOrchestrator.class);

    @Value("${cb.max.salt.new.service.retry}")
    private int maxRetry;

    @Value("${cb.max.salt.new.service.leave.retry}")
    private int maxRetryLeave;

    @Value("${cb.max.salt.recipe.execution.retry}")
    private int maxRetryRecipe;

    @Value("${cb.max.salt.recipe.execution.retry.forced:2}")
    private int maxRetryRecipeForced;

    @Value("${cb.max.salt.database.dr.retry:300}")
    private int maxDatabaseDrRetryDefault;

    @Value("${cb.max.salt.database.dr.retry.onerror:5}")
    private int maxDatabaseDrRetryOnError;

    @Value("${cb.max.salt.cloudstorage.validation.retry:3}")
    private int maxCloudStorageValidationRetry;

    @Inject
    private SaltRunner saltRunner;

    @Inject
    private SaltCommandRunner saltCommandRunner;

    @Inject
    private GrainUploader grainUploader;

    @Inject
    private Retry retry;

    @Inject
    private ExitCriteria exitCriteria;

    @Inject
    private SaltService saltService;

    @Inject
    private CompressUtil compressUtil;

    @Inject
    private SaltStateService saltStateService;

    @Inject
    private SaltBootstrapFactory saltBootstrapFactory;

    @Override
    public void bootstrap(List<GatewayConfig> allGatewayConfigs, Set<Node> targets, BootstrapParams params,
            ExitCriteriaModel exitModel) throws CloudbreakOrchestratorException {
        LOGGER.debug("Start SaltBootstrap on nodes: {}", targets);
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGatewayConfigs);
        List<SaltConnector> saltConnectors = saltService.createSaltConnector(allGatewayConfigs);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            uploadSaltMasterConfig(sc, params, gatewayTargets, exitModel);
            uploadSaltConfig(sc, gatewayTargets, exitModel);
            Set<String> allTargets = targets.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            uploadSignKey(sc, primaryGateway, gatewayTargets, allTargets, exitModel);
            OrchestratorBootstrap saltBootstrap = saltBootstrapFactory.of(sc, saltConnectors, allGatewayConfigs, targets, params);
            Callable<Boolean> saltBootstrapRunner = saltRunner.runnerWithConfiguredErrorCount(saltBootstrap, exitCriteria, exitModel);
            saltBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during the salt bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        } finally {
            saltConnectors.forEach(SaltConnector::close);
        }
        LOGGER.debug("SaltBootstrap finished");
    }

    private void uploadSaltMasterConfig(SaltConnector sc, BootstrapParams params, Set<String> gatewayTargets, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        String masterConfig = "worker_threads: " + params.getMasterWorkerThreads();
        uploadFileToTargets(sc, gatewayTargets, exitModel, "/etc/salt/master.d", "worker_threads.conf", masterConfig.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void changePassword(List<GatewayConfig> allGatewayConfigs, String newPassword, String oldPassword) throws CloudbreakOrchestratorException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGatewayConfigs);
        LOGGER.info("Changing salt password using primary gateway {} on all gateways: {} ", primaryGateway.getPrivateAddress(), gatewayTargets);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            GenericResponses responses = saltStateService.changePassword(sc, gatewayTargets, newPassword);
            List<GenericResponse> errorResponses = responses.getResponses().stream()
                    .filter(response -> response.getStatusCode() != HttpStatus.OK.value())
                    .collect(Collectors.toList());
            if (!errorResponses.isEmpty()) {
                LOGGER.warn("Change password responses contain error(s): {}", errorResponses);
                boolean partialSuccess = errorResponses.size() != responses.getResponses().size();
                if (partialSuccess) {
                    boolean changeBackSuccess = tryToChangeBackPasswordOnSuccessNodes(sc, responses, oldPassword);
                    if (changeBackSuccess) {
                        partialSuccess = false;
                    }
                }
                String message = partialSuccess
                        ? "Failed to change password on some of the nodes, so you may experience issues with the cluster until the password is fixed."
                        : "Failed to change password on some of the nodes, but successfully reverted back to old password so cluster health is not affected.";
                message += " Please check the salt-bootstrap service status on nodes and retry the operation. Details from nodes: " +
                        errorResponses.stream()
                                .map(response -> String.format("%s: HTTP %s %s", response.getAddress(), response.getStatusCode(), response.getErrorText()))
                                .collect(Collectors.joining(", "));
                throw new CloudbreakOrchestratorFailedException(message);
            }
        } catch (CloudbreakOrchestratorException e) {
            throw e;
        } catch (WebApplicationException e) {
            LOGGER.warn("Error occurred during salt password change", e);
            throw new CloudbreakOrchestratorFailedException("Salt-bootstrap responded with error, please check the service status on node " +
                    primaryGateway.getPrivateAddress() + " and retry the operation. Details: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.warn("Error occurred during salt password change", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private boolean tryToChangeBackPasswordOnSuccessNodes(SaltConnector sc, GenericResponses responses, String oldPassword) {
        LOGGER.info("Trying to change back the password to old value after partial password change success");
        try {
            Set<String> gatewaysWithSuccessfulPasswordChange = responses.getResponses().stream()
                    .filter(response -> response.getStatusCode() == HttpStatus.OK.value())
                    .map(GenericResponse::getAddress)
                    .collect(Collectors.toSet());
            GenericResponses changeBackResponses = saltStateService.changePassword(sc, gatewaysWithSuccessfulPasswordChange, oldPassword);
            boolean changedBackOnAllSuccessNodes = changeBackResponses.getResponses().stream()
                    .allMatch(response -> response.getStatusCode() == HttpStatus.OK.value());
            if (changedBackOnAllSuccessNodes) {
                LOGGER.info("Changed back old password on all success nodes");
                return true;
            } else {
                LOGGER.warn("Failed to change back password on some nodes. Responses: {}", changeBackResponses.getResponses());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to change back old password", e);
        }
        return false;
    }

    @Override
    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public Map<String, Map<String, String>> formatAndMountDisksOnNodes(OrchestratorAware stack, List<GatewayConfig> allGateway, Set<Node> nodesWithDiskData,
            Set<Node> allNodes, ExitCriteriaModel exitModel, String platformVariant) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateway);
        Set<String> gatewayTargetIpAddresses = getGatewayPrivateIps(allGateway);
        Target<String> allHosts = new HostList(nodesWithDiskData.stream().map(Node::getHostname).collect(Collectors.toSet()));
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            saveHostsPillar(stack, exitModel, gatewayTargetIpAddresses, sc);
            Callable<Boolean> saltPillarRunner;

            Map<String, String> dataVolumeMap = nodesWithDiskData.stream()
                    .collect(Collectors.toMap(Node::getHostname, node -> node.getNodeVolumes().getDataVolumes()));
            Map<String, String> serialIdMap = nodesWithDiskData.stream()
                    .collect(Collectors.toMap(Node::getHostname, node -> node.getNodeVolumes().getSerialIds()));
            Map<String, String> fstabMap = nodesWithDiskData.stream()
                    .collect(Collectors.toMap(Node::getHostname, node -> node.getNodeVolumes().getFstab()));
            Map<String, String> temporaryStorageMap = nodesWithDiskData.stream()
                    .collect(Collectors.toMap(Node::getHostname, node -> node.getTemporaryStorage().name()));
            Map<String, Integer> dataBaseVolumeIndexMap =
                    nodesWithDiskData.stream().collect(Collectors.toMap(Node::getHostname, node -> node.getNodeVolumes().getDatabaseVolumeIndex()));

            Map<String, Object> hostnameDiskMountMap = nodesWithDiskData.stream().map(Node::getHostname).collect(Collectors.toMap(hn -> hn, hn -> Map.of(
                    "attached_volume_name_list", dataVolumeMap.getOrDefault(hn, ""),
                    "attached_volume_serial_list", serialIdMap.getOrDefault(hn, ""),
                    "cloud_platform", platformVariant,
                    "previous_fstab", fstabMap.getOrDefault(hn, ""),
                    "database_volume_index", dataBaseVolumeIndexMap.getOrDefault(hn, -1),
                    "temporary_storage", temporaryStorageMap.getOrDefault(hn, TemporaryStorage.ATTACHED_VOLUMES.name())
            )));

            SaltPillarProperties mounDiskProperties =
                    new SaltPillarProperties("/mount/disk.sls", Collections.singletonMap("mount_data", hostnameDiskMountMap));

            OrchestratorBootstrap pillarSave = PillarSave.createCustomPillar(sc, gatewayTargetIpAddresses, mounDiskProperties);
            saltPillarRunner = saltRunner.runnerWithConfiguredErrorCount(pillarSave, exitCriteria, exitModel);
            saltPillarRunner.call();

            saltCommandRunner.runModifyGrainCommand(sc,
                    new GrainAddRunner(saltStateService, hostnameDiskMountMap.keySet(), allNodes, "mount_disks"), exitModel, exitCriteria);

            StateAllRunner stateAllRunner = new StateAllRunner(saltStateService, gatewayTargetIpAddresses, allNodes, "disks.format-and-mount");
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(saltStateService, sc, stateAllRunner);
            Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runner(saltJobIdTracker, exitCriteria, exitModel);
            saltJobRunBootstrapRunner.call();

            Map<String, String> uuidResponse = saltStateService.getUuidList(sc);

            saltCommandRunner.runModifyGrainCommand(sc,
                    new GrainRemoveRunner(saltStateService, hostnameDiskMountMap.keySet(), allNodes, "mount_disks"), exitModel, exitCriteria);
            Map<String, String> fstabResponse = saltStateService.runCommandOnHosts(retry, sc, allHosts, "cat /etc/fstab");
            return nodesWithDiskData.stream()
                    .map(node -> {
                        String fstab = fstabResponse.getOrDefault(node.getHostname(), "");
                        String uuidList = uuidResponse.getOrDefault(node.getHostname(), "");
                        return new SimpleImmutableEntry<>(node.getHostname(), Map.of("uuids", uuidList, "fstab", fstab));
                    })
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        } catch (Exception e) {
            LOGGER.info("Error occurred during the salt bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private void saveHostsPillar(OrchestratorAware stack, ExitCriteriaModel exitModel,
            Set<String> gatewayTargetIpAddresses, SaltConnector sc) throws Exception {
        OrchestratorBootstrap hostSave = PillarSave.createHostsPillar(sc, gatewayTargetIpAddresses, stack.getAllFunctioningNodes());
        Callable<Boolean> saltPillarRunner = saltRunner.runnerWithConfiguredErrorCount(hostSave, exitCriteria, exitModel);
        saltPillarRunner.call();
    }

    private Map<String, String> mountDisks(String platformVariant, SaltConnector sc, Glob hostname, String uuidList, String fstab) {
        String mountCommandParams = "CLOUD_PLATFORM='" + platformVariant + "' ATTACHED_VOLUME_UUID_LIST='" + uuidList + "' ";

        if (StringUtils.isNotEmpty(fstab)) {
            mountCommandParams += "PREVIOUS_FSTAB='" + fstab + "' ";
        }
        saltStateService.runCommandOnHosts(retry, sc, hostname, "(cd " + SRV_SALT_DISK + ';' + mountCommandParams + " ./" + DISK_MOUNT + ')');
        return StringUtils.isEmpty(uuidList) ? Map.of() : saltStateService.runCommandOnHosts(retry, sc, hostname, "cat /etc/fstab");
    }

    private String formatDisks(String platformVariant, SaltConnector sc, Node node, Glob hostname) {
        NodeVolumes nodeVolumes = node.getNodeVolumes();
        if (StringUtils.isNotEmpty(nodeVolumes.getFstab())) {
            return nodeVolumes.getUuids();
        }

        String dataVolumes = String.join(" ", nodeVolumes.getDataVolumes());
        String serialIds = String.join(" ", nodeVolumes.getSerialIds());
        String formatCommandParams = "CLOUD_PLATFORM='" + platformVariant
                + "' ATTACHED_VOLUME_NAME_LIST='" + dataVolumes
                + "' ATTACHED_VOLUME_SERIAL_LIST='" + serialIds + "' ";
        String command = "(cd " + SRV_SALT_DISK + ';' + formatCommandParams + "./" + DISK_FORMAT + ')';
        Map<String, String> formatResponse = saltStateService.runCommandOnHosts(retry, sc, hostname, command);
        return formatResponse.getOrDefault(node.getHostname(), "");
    }

    private void uploadMountScriptsAndMakeThemExecutable(Set<Node> nodes, ExitCriteriaModel exitModel, Set<String> allTargets, Target<String> allHosts,
            SaltConnector sc) throws IOException {
        Map.of(
                        DISK_INITIALIZE, readFileFromClasspath(DISK_SCRIPT_PATH + DISK_INITIALIZE).getBytes(),
                        DISK_COMMON, readFileFromClasspath(DISK_SCRIPT_PATH + DISK_COMMON).getBytes(),
                        DISK_FORMAT, readFileFromClasspath(DISK_SCRIPT_PATH + DISK_FORMAT).getBytes(),
                        DISK_MOUNT, readFileFromClasspath(DISK_SCRIPT_PATH + DISK_MOUNT).getBytes())
                .entrySet()
                .stream()
                .map(script -> {
                    String scriptName = script.getKey();
                    try {
                        LOGGER.debug("Uploading script {} to targets {}", scriptName, nodes);
                        uploadFileToTargets(sc, allTargets, exitModel, SRV_SALT_DISK, scriptName, script.getValue());
                        return SRV_SALT_DISK + '/' + scriptName;
                    } catch (CloudbreakOrchestratorFailedException e) {
                        String message = String.format("Failed to upload file %s, to targets %s", scriptName, allTargets.toString());
                        throw new CloudbreakServiceException(message, e);
                    }
                })
                .forEach(path -> {
                    LOGGER.debug("Making script {} executable on targets {}", path, nodes);
                    saltStateService.runCommandOnHosts(retry, sc, allHosts, "chmod 755 " + path);
                });
    }

    @Override
    public void bootstrapNewNodes(List<GatewayConfig> allGatewayConfigs, Set<Node> targets, Set<Node> allNodes, byte[] stateConfigZip, BootstrapParams params,
            ExitCriteriaModel exitModel) throws CloudbreakOrchestratorException {
        LOGGER.info("Bootstrap new nodes: {}", targets);
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargets = allGatewayConfigs.stream().filter(gc -> targets.stream().anyMatch(n -> gc.getPrivateAddress().equals(n.getPrivateIp())))
                .map(GatewayConfig::getPrivateAddress).collect(Collectors.toSet());
        List<SaltConnector> saltConnectors = saltService.createSaltConnector(allGatewayConfigs);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            if (!gatewayTargets.isEmpty()) {
                LOGGER.info("Gateway targets are not empty, upload salt config: {}", gatewayTargets);
                uploadSaltMasterConfig(sc, params, gatewayTargets, exitModel);
                uploadSaltConfig(sc, gatewayTargets, stateConfigZip, exitModel);
                params.setRestartNeeded(true);
            }
            uploadSignKey(sc, primaryGateway, gatewayTargets, targets.stream().map(Node::getPrivateIp).collect(Collectors.toSet()), exitModel);
            // if there is a new salt master then re-bootstrap all nodes
            Set<Node> nodes = gatewayTargets.isEmpty() ? targets : allNodes;
            OrchestratorBootstrap saltBootstrap = saltBootstrapFactory.of(sc, saltConnectors, allGatewayConfigs, nodes, params);
            Callable<Boolean> saltBootstrapRunner = saltRunner.runner(saltBootstrap, exitCriteria, exitModel);
            saltBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during salt upscale", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        } finally {
            saltConnectors.forEach(SaltConnector::close);
        }
    }

    @Override
    public void reBootstrapExistingNodes(List<GatewayConfig> allGatewayConfigs, Set<Node> targets, BootstrapParams params,
            ExitCriteriaModel exitModel) throws CloudbreakOrchestratorException {
        LOGGER.info("Re-bootstrap existing nodes: {}", targets.stream().map(Node::getHostname).collect(Collectors.toSet()));
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGatewayConfigs);
        List<SaltConnector> saltConnectors = saltService.createSaltConnector(allGatewayConfigs);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            GenericResponses genericResponses = saltStateService.bootstrap(sc, params, allGatewayConfigs, targets);
            List<GenericResponse> errorResponses = genericResponses.getResponses().stream()
                    .filter(response -> response.getStatusCode() != HttpStatus.OK.value())
                    .collect(Collectors.toList());
            if (!errorResponses.isEmpty()) {
                Set<String> nodesWithErrors = errorResponses.stream().map(GenericResponse::getAddress).collect(Collectors.toSet());
                LOGGER.error("Failed to rebootstrap existing nodes [{}/{}] {}", targets.size(), nodesWithErrors.size(), errorResponses);
                throw new CloudbreakOrchestratorFailedException(String.format("Failed to rebootstrap existing nodes %s", nodesWithErrors));
            }
        } catch (Exception e) {
            LOGGER.info("Error occurred during salt upscale", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        } finally {
            saltConnectors.forEach(SaltConnector::close);
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void initServiceRun(OrchestratorAware stack, List<GatewayConfig> allGateway, Set<Node> allNodes, Set<Node> reachableNodes, SaltConfig saltConfig,
            ExitCriteriaModel exitModel, String cloudPlatform) throws CloudbreakOrchestratorException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateway);
        Set<String> gatewayTargetIpAddresses = getGatewayPrivateIps(allGateway);
        Set<String> gatewayTargetHostnames = getGatewayHostnames(allGateway);
        Set<String> serverHostname = Sets.newHashSet(primaryGateway.getHostname());
        Set<String> reachableHostnames = reachableNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            saveHostsPillar(stack, exitModel, gatewayTargetIpAddresses, sc);
            saveCustomPillars(saltConfig, exitModel, gatewayTargetIpAddresses, sc);

            setAdMemberRoleIfNeeded(allNodes, saltConfig, exitModel, sc, reachableHostnames);
            setIpaMemberRoleIfNeeded(allNodes, saltConfig, exitModel, sc, reachableHostnames);

            // knox
            if (primaryGateway.getKnoxGatewayEnabled()) {
                saltCommandRunner.runModifyGrainCommand(sc,
                        new GrainAddRunner(saltStateService, gatewayTargetHostnames, allNodes, "gateway"), exitModel, exitCriteria);
            }

            setPostgreRoleIfNeeded(allNodes, saltConfig, exitModel, sc, serverHostname);

            addClusterManagerRoles(allNodes, exitModel, sc, serverHostname, reachableHostnames);

            // kerberos
            if (saltConfig.getServicePillarConfig().containsKey("kerberos")) {
                saltCommandRunner.runModifyGrainCommand(sc,
                        new GrainAddRunner(saltStateService, reachableHostnames, allNodes, "kerberized"), exitModel, exitCriteria);
            }
            grainUploader.uploadGrains(allNodes, saltConfig.getGrainsProperties(), exitModel, sc, exitCriteria);

            runSyncAll(sc, reachableHostnames, allNodes, exitModel);
            saltCommandRunner.runSaltCommand(sc, new MineUpdateRunner(saltStateService, gatewayTargetHostnames, allNodes), exitModel, exitCriteria);
        } catch (ExecutionException e) {
            LOGGER.warn("Error occurred during bootstrap", e);
            if (e.getCause() instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) e.getCause();
            }
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);

        } catch (Exception e) {
            LOGGER.warn("Error occurred during bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    public void saveCustomPillars(SaltConfig saltConfig, ExitCriteriaModel exitModel, OrchestratorStateParams stateParams)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = stateParams.getPrimaryGatewayConfig();
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            saveCustomPillars(saltConfig, exitModel, stateParams.getTargetHostNames(), sc);
        } catch (Exception e) {
            LOGGER.info("Error occurred during save custom pillars", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public Optional<MemoryInfo> getMemoryInfo(GatewayConfig gatewayConfig) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig)) {
            return saltStateService.getMemoryInfo(sc, gatewayConfig.getHostname());
        } catch (Exception e) {
            LOGGER.info("Error occurred during requesting memory information", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public Optional<Memory> getClusterManagerMemory(GatewayConfig gatewayConfig) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig)) {
            return saltStateService.getClouderaManagerMemory(sc, gatewayConfig);
        } catch (Exception e) {
            LOGGER.info("Error occurred during requesting memory information", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void setClusterManagerMemory(GatewayConfig gatewayConfig, Memory memory) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig)) {
            saltStateService.setClouderaManagerMemory(sc, gatewayConfig, memory);
        } catch (Exception e) {
            LOGGER.info("Error occurred during setting cluster manager memory", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private void saveCustomPillars(SaltConfig saltConfig, ExitCriteriaModel exitModel, Set<String> gatewayTargetIpAddresses,
            SaltConnector sc) throws Exception {

        for (Entry<String, SaltPillarProperties> propertiesEntry : saltConfig.getServicePillarConfig().entrySet()) {
            OrchestratorBootstrap pillarSave = PillarSave.createCustomPillar(sc, gatewayTargetIpAddresses, propertiesEntry.getValue());
            Callable<Boolean> saltPillarRunner = saltRunner.runnerWithConfiguredErrorCount(pillarSave, exitCriteria, exitModel);
            saltPillarRunner.call();
        }
    }

    @Override
    public void initSaltConfig(OrchestratorAware stack, List<GatewayConfig> allGateway, Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateway);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGateway);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            saveHostsPillar(stack, exitModel, gatewayTargets, sc);
            saveCustomPillars(saltConfig, exitModel, gatewayTargets, sc);
            runSyncAll(sc, gatewayTargets, allNodes, exitModel);
        } catch (ExecutionException e) {
            LOGGER.warn("Error occurred during bootstrap", e);
            if (e.getCause() instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) e.getCause();
            }
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);

        } catch (Exception e) {
            LOGGER.warn("Error occurred during bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private void addClusterManagerRoles(Set<Node> allNodes, ExitCriteriaModel exitModel,
            SaltConnector sc, Set<String> serverHostnames, Set<String> reachableHostnames) throws Exception {
        saltCommandRunner.runModifyGrainCommand(sc,
                new GrainAddRunner(saltStateService, reachableHostnames, allNodes, "manager_agent"), exitModel, exitCriteria);
        saltCommandRunner.runModifyGrainCommand(sc,
                new GrainAddRunner(saltStateService, serverHostnames, allNodes, "manager_server"), exitModel, exitCriteria);
    }

    private void setAdMemberRoleIfNeeded(Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel, SaltConnector sc,
            Set<String> reachableHostnames) throws Exception {
        if (saltConfig.getServicePillarConfig().containsKey("sssd-ad")) {
            saltCommandRunner.runModifyGrainCommand(sc,
                    new GrainAddRunner(saltStateService, reachableHostnames, allNodes, "ad_member"), exitModel, exitCriteria);
        }
    }

    private void setIpaMemberRoleIfNeeded(Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel, SaltConnector sc,
            Set<String> reachableHostnames) throws Exception {
        if (saltConfig.getServicePillarConfig().containsKey("sssd-ipa")) {
            saltCommandRunner.runModifyGrainCommand(sc,
                    new GrainAddRunner(saltStateService, reachableHostnames, allNodes, "ipa_member"), exitModel, exitCriteria);
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void runService(List<GatewayConfig> allGateway, Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorException {
        LOGGER.debug("Run Services on nodes: {}", allNodes);
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateway);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            retry.testWith2SecDelayMax5Times(() -> getRolesBeforeHighstateMagic(sc));
            Set<String> allHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
            runNewService(sc, new HighStateRunner(saltStateService, allHostnames, allNodes), exitModel);
        } catch (ExecutionException e) {
            LOGGER.info("Error occurred during bootstrap", e);
            if (e.getCause() instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) e.getCause();
            }
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.info("Error occurred during bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
        LOGGER.debug("Run services on nodes finished: {}", allNodes);
    }

    private void getRolesBeforeHighstateMagic(SaltConnector sc) {
        try {
            // YARN/SALT MAGIC: If you remove 'get role grains' before highstate, then highstate can run with defective roles,
            // so it can happen that some roles will be missing on some nodes. Please do not delete only if you know what you are doing.
            Map<String, JsonNode> roles = saltStateService.getGrains(sc, "roles");
            LOGGER.info("Roles before highstate: " + roles);
        } catch (RuntimeException e) {
            LOGGER.info("Can't get roles before highstate", e);
            throw new Retry.ActionFailedException("Can't get roles before highstate: " + e.getMessage());
        }
    }

    private void setPostgreRoleIfNeeded(Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel, SaltConnector sc, Set<String> serverHostname)
            throws Exception {
        if (saltConfig.getServicePillarConfig().containsKey("postgresql-server")) {
            saltCommandRunner.runModifyGrainCommand(sc,
                    new GrainAddRunner(saltStateService, serverHostname, allNodes, "postgresql_server"), exitModel, exitCriteria);
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void changePrimaryGateway(GatewayConfig formerGateway, GatewayConfig newPrimaryGateway, List<GatewayConfig> allGatewayConfigs, Set<Node> allNodes,
            ExitCriteriaModel exitCriteriaModel) {
        LOGGER.debug("Change primary gateway is not implemented, yet");
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void validateCloudStorageBackup(GatewayConfig primaryGateway, List<GatewayConfig> allGatewayConfigs, Set<Node> allNodes,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        Set<String> targetHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            runValidation(sc, new StateAllRunner(saltStateService, targetHostnames, allNodes, "validation/cloud-storage-backup"), exitCriteriaModel);
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
        Callable<Boolean> saltJobRunBootstrapRunner =
                saltRunner.runnerWithCalculatedErrorCount(saltJobIdTracker, exitCriteria, exitCriteriaModel, maxCloudStorageValidationRetry);
        saltJobRunBootstrapRunner.call();
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void installFreeIpa(GatewayConfig primaryGateway, List<GatewayConfig> allGatewayConfigs, Set<Node> allNodes,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        Set<String> freeIpaMasterHostname = new HashSet<>(
                getHostnamesForRoles(primaryGateway, Set.of(FREEIPA_MASTER_ROLE, FREEIPA_MASTER_REPLACEMENT_ROLE), allNodes));
        Set<String> existingFreeIpaReplicaHostnames = new HashSet<>(getHostnamesForRoles(primaryGateway, Set.of(FREEIPA_REPLICA_ROLE), allNodes));

        Set<String> unassignedHostnames = allGatewayConfigs.stream()
                .map(GatewayConfig::getHostname)
                .filter(hostname -> !freeIpaMasterHostname.contains(hostname))
                .filter(hostname -> !existingFreeIpaReplicaHostnames.contains(hostname))
                .collect(Collectors.toCollection(HashSet::new));

        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            installFreeIpaUpdateExistingReplicas(sc, existingFreeIpaReplicaHostnames, allNodes, exitCriteriaModel);

            unassignedHostnames.removeAll(installFreeIpaPrimary(sc, primaryGateway, freeIpaMasterHostname, unassignedHostnames, existingFreeIpaReplicaHostnames,
                    allNodes, exitCriteriaModel));

            installFreeIpaReplicas(sc, unassignedHostnames, allNodes, exitCriteriaModel);

            LOGGER.debug("Completed installing FreeIPA");
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.warn("CloudbreakOrchestratorException occurred during FreeIPA installation", e);
            throw e;
        } catch (ExecutionException e) {
            LOGGER.warn("Error occurred during FreeIPA installation", e);
            if (e.getCause() instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) e.getCause();
            }
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.warn("Error occurred during FreeIPA installation", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private void installFreeIpaUpdateExistingReplicas(SaltConnector sc, Set<String> existingFreeIpaReplicaHostnames, Set<Node> allNodes,
            ExitCriteriaModel exitCriteriaModel) throws Exception {
        LOGGER.debug("Exsting Replica FreeIPAs: [{}]", existingFreeIpaReplicaHostnames);
        // The existing replicas need to be serialized into high state. See the comments in CB-7335 for more details.
        for (String existingReplicaHostname : existingFreeIpaReplicaHostnames) {
            LOGGER.debug("Applying changes to FreeIPA replica {}", existingReplicaHostname);
            runNewService(sc, new HighStateRunner(saltStateService, Set.of(existingReplicaHostname), allNodes), exitCriteriaModel);
        }
    }

    private void installFreeIpaReplicas(SaltConnector sc, Set<String> newFreeIpaReplicaHostnames, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws Exception {
        LOGGER.debug("New Replica FreeIPAs: {}", newFreeIpaReplicaHostnames);
        for (String newFreeIpaReplicaHostname : newFreeIpaReplicaHostnames) {
            LOGGER.debug("New Replica FreeIPA: [{}]", newFreeIpaReplicaHostname);
            saltCommandRunner.runModifyGrainCommand(sc,
                    new GrainAddRunner(saltStateService, Set.of(newFreeIpaReplicaHostname), allNodes, FREEIPA_REPLICA_ROLE), exitCriteriaModel, exitCriteria);
            runNewService(sc, new HighStateRunner(saltStateService, Set.of(newFreeIpaReplicaHostname), allNodes), exitCriteriaModel);
            LOGGER.debug("New Replica FreeIPA installation finished for: [{}]", newFreeIpaReplicaHostname);
        }
    }

    private Set<String> installFreeIpaPrimary(SaltConnector sc, GatewayConfig primaryGateway, Set<String> freeIpaMasterHostname,
            Set<String> unassignedHostnames, Set<String> existingFreeIpaReplicaHostnames, Set<Node> allNodes,
            ExitCriteriaModel exitCriteriaModel) throws Exception {

        Set<String> freeIpaMasterHostnames = new HashSet<>(freeIpaMasterHostname);
        if (!freeIpaMasterHostnames.isEmpty()) {
            LOGGER.debug("Existing primary FreeIPA: {}", freeIpaMasterHostnames);
        } else if (existingFreeIpaReplicaHostnames.isEmpty()) {
            selectPrimaryGwAsFreeIpaMasterForInitialDeployment(sc, primaryGateway, allNodes, exitCriteriaModel, freeIpaMasterHostnames);
        } else {
            selectNewMasterReplacement(sc, primaryGateway, unassignedHostnames, existingFreeIpaReplicaHostnames, allNodes, exitCriteriaModel,
                    freeIpaMasterHostnames);
        }
        runNewService(sc, new HighStateRunner(saltStateService, freeIpaMasterHostnames, allNodes), exitCriteriaModel);
        return freeIpaMasterHostnames;
    }

    private void selectPrimaryGwAsFreeIpaMasterForInitialDeployment(SaltConnector sc, GatewayConfig primaryGateway, Set<Node> allNodes,
            ExitCriteriaModel exitCriteriaModel, Set<String> freeIpaMasterHostnames) throws Exception {
        freeIpaMasterHostnames.add(primaryGateway.getHostname());
        LOGGER.debug("Initial primary FreeIPA: {}", freeIpaMasterHostnames);
        saltCommandRunner.runModifyGrainCommand(sc,
                new GrainAddRunner(saltStateService, freeIpaMasterHostnames, allNodes, FREEIPA_MASTER_ROLE), exitCriteriaModel, exitCriteria);
    }

    private void selectNewMasterReplacement(SaltConnector sc, GatewayConfig primaryGateway, Set<String> unassignedHostnames,
            Set<String> existingFreeIpaReplicaHostnames, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel, Set<String> freeIpaMasterHostnames)
            throws Exception {
        freeIpaMasterHostnames.add(unassignedHostnames.stream().findFirst()
                .orElseGet(() -> choosePrimaryGatewayAsMasterReplacement(primaryGateway, existingFreeIpaReplicaHostnames)
                        .orElseThrow(() ->
                                new NotFoundException("A primary FreeIPA instance is required and there are no unassigned roles to assign as a primary"))));
        LOGGER.debug("Replacement primary FreeIPA: {}", freeIpaMasterHostnames);
        saltCommandRunner.runModifyGrainCommand(sc,
                new GrainAddRunner(saltStateService, freeIpaMasterHostnames, allNodes, FREEIPA_MASTER_REPLACEMENT_ROLE), exitCriteriaModel, exitCriteria);
        saltCommandRunner.runModifyGrainCommand(sc,
                new GrainRemoveRunner(saltStateService, freeIpaMasterHostnames, allNodes, FREEIPA_REPLICA_ROLE), exitCriteriaModel, exitCriteria);
    }

    private Optional<String> choosePrimaryGatewayAsMasterReplacement(GatewayConfig primaryGateway, Set<String> existingFreeIpaReplicaHostnames) {
        LOGGER.debug("No new candidate found. Trying to choose the primary gateway [{}] from existing replicas: {}",
                primaryGateway.getHostname(), existingFreeIpaReplicaHostnames);
        return existingFreeIpaReplicaHostnames.stream().filter(replica -> primaryGateway.getHostname().equals(replica)).findFirst();
    }

    @Override
    public Optional<String> getFreeIpaMasterHostname(GatewayConfig primaryGateway, Set<Node> allNodes) throws CloudbreakOrchestratorException {
        return getHostnamesForRoles(primaryGateway, Set.of(FREEIPA_MASTER_ROLE, FREEIPA_MASTER_REPLACEMENT_ROLE), allNodes).stream().findFirst();
    }

    private Set<String> getHostnamesForRoles(GatewayConfig primaryGateway, Set<String> rolesToSearch, Set<Node> allNodes)
            throws CloudbreakOrchestratorFailedException {
        Set<String> hostnames = new HashSet<>();
        for (String roleToSearch : rolesToSearch) {
            hostnames.addAll(getHostnamesForRole(primaryGateway, roleToSearch, allNodes));
        }
        return hostnames;
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    private Set<String> getHostnamesForRole(GatewayConfig primaryGateway, String roleToSearch, Set<Node> allNodes)
            throws CloudbreakOrchestratorFailedException {
        Map<String, JsonNode> roles;
        Collection<String> targetHosts = allNodes.stream()
                .map(Node::getHostname)
                .collect(Collectors.toSet());
        if (targetHosts.isEmpty()) {
            LOGGER.warn("No hosts to get role for");
            roles = Map.of();
        } else {
            try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
                roles = saltStateService.getGrains(sc, new HostAndRoleTarget(roleToSearch, targetHosts), "roles");
            } catch (Exception e) {
                LOGGER.warn("Error occurred when getting roles", e);
                throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
            }
        }
        return roles.keySet();
    }

    @Override
    public void resetClusterManager(GatewayConfig gatewayConfig, Set<String> target, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel) {
        LOGGER.debug("Cluster manager reset is not implemented, yet");
    }

    @Override
    public void stopClusterManagerOnMaster(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel) {
        LOGGER.debug("Stop cluster manager is not implemented, yet");
    }

    @Override
    public void startClusterManagerOnMaster(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel) {
        LOGGER.debug("Start cluster manager is not implemented, yet");
    }

    @Override
    public void restartClusterManagerOnMaster(GatewayConfig gatewayConfig, Set<String> target, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig)) {
            StateRunner stateRunner = new StateRunner(saltStateService, target, CM_SERVER_RESTART);
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(saltStateService, sc, stateRunner);
            Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runnerWithConfiguredErrorCount(saltJobIdTracker, exitCriteria, exitCriteriaModel);
            saltJobRunBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.error("Error occurred during CM Server restart", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void startClusterManagerWithItsAgents(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        LOGGER.debug("Start Cluster manager and its agents: [{}]", allNodes);
        runHighStateWithSpecificRole(gatewayConfig, allNodes, exitCriteriaModel, "cloudera_manager_full_start", "Error occurred during CM start");
    }

    @Override
    public void stopClusterManagerWithItsAgents(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        LOGGER.debug("Stop Cluster manager and its agents: [{}]", allNodes);
        runHighStateWithSpecificRole(gatewayConfig, allNodes, exitCriteriaModel, "cloudera_manager_full_stop", "Error occurred during CM stop");
    }

    private void runHighStateWithSpecificRole(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel, String role,
            String errorMsg) throws CloudbreakOrchestratorException {
        Set<String> targets = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig)) {
            runHighStateWithSpecificRole(allNodes, exitCriteriaModel, role, targets, sc);
        } catch (Exception e) {
            LOGGER.error(errorMsg, e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private void runHighStateWithSpecificRole(Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel, String role, Set<String> targets, SaltConnector sc)
            throws Exception {
        try {
            saltCommandRunner.runModifyGrainCommand(sc,
                    new GrainAddRunner(saltStateService, targets, allNodes, role), exitCriteriaModel, exitCriteria);
            runNewService(sc, new HighStateRunner(saltStateService, targets, allNodes), exitCriteriaModel);
        } finally {
            saltCommandRunner.runModifyGrainCommand(sc,
                    new GrainRemoveRunner(saltStateService, targets, allNodes, role), exitCriteriaModel, exitCriteria);
        }
    }

    @Override
    public void updateAgentCertDirectoryPermission(GatewayConfig gatewayConfig, Set<String> target, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig)) {
            StateRunner stateRunner = new StateRunner(saltStateService, target, CM_AGENT_CERTDIR_PERMISSION);
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(saltStateService, sc, stateRunner);
            Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runnerWithConfiguredErrorCount(saltJobIdTracker, exitCriteria, exitCriteriaModel);
            saltJobRunBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.error("Error occurred during CM Agent certdir permission update", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void upgradeClusterManager(GatewayConfig gatewayConfig, Set<String> target, Set<Node> allNodes, SaltConfig pillarConfig,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig)) {
            for (Entry<String, SaltPillarProperties> propertiesEntry : pillarConfig.getServicePillarConfig().entrySet()) {
                OrchestratorBootstrap pillarSave =
                        PillarSave.createCustomPillar(sc, Sets.newHashSet(gatewayConfig.getPrivateAddress()), propertiesEntry.getValue());
                Callable<Boolean> saltPillarRunner = saltRunner.runnerWithConfiguredErrorCount(pillarSave, exitCriteria, exitCriteriaModel);
                saltPillarRunner.call();
            }

            // add 'manager_upgrade' role to all nodes
            Set<String> targetHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
            saltCommandRunner.runModifyGrainCommand(sc, new GrainAddRunner(saltStateService, targetHostnames, allNodes, "roles", "manager_upgrade"),
                    exitCriteriaModel, exitCriteria);

            Set<String> allHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
            runSyncAll(sc, allHostnames, allNodes, exitCriteriaModel);
            runNewService(sc, new HighStateRunner(saltStateService, allHostnames, allNodes), exitCriteriaModel, maxRetry, true);

            // remove 'manager_upgrade' role from all nodes
            saltCommandRunner.runModifyGrainCommand(sc, new GrainRemoveRunner(saltStateService, targetHostnames, allNodes, "roles", "manager_upgrade"),
                    exitCriteriaModel, exitCriteria);
        } catch (Exception e) {
            LOGGER.error("Error occurred during Cloudera Manager upgrade", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    @Override
    public void tearDown(OrchestratorAware stack, List<GatewayConfig> allGatewayConfigs, Map<String, String> removeNodePrivateIPsByFQDN,
            Set<Node> remainingNodes, ExitCriteriaModel exitModel) throws CloudbreakOrchestratorException {
        LOGGER.debug("Tear down hosts: {},", removeNodePrivateIPsByFQDN);
        LOGGER.debug("Gateway config for tear down: {}", allGatewayConfigs);
        Set<String> remainingIps = remainingNodes.stream()
                .map(Node::getPrivateIp)
                .collect(Collectors.toSet());
        LOGGER.debug("Remaining IPs: {}", remainingIps);
        Set<String> minionsToStop = removeNodePrivateIPsByFQDN.values().stream()
                .filter(not(remainingIps::contains))
                .collect(Collectors.toSet());
        LOGGER.debug("Minions to stop: {}", minionsToStop);
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargetIpAddresses = getGatewayPrivateIps(allGatewayConfigs);
        try (SaltConnector saltConnector = saltService.createSaltConnector(primaryGateway)) {
            saltStateService.stopMinions(saltConnector, minionsToStop);
            if (!CollectionUtils.isEmpty(remainingNodes)) {
                saveHostsPillar(stack, exitModel, gatewayTargetIpAddresses, saltConnector);
            }
        } catch (Exception e) {
            LOGGER.info("Error occurred during salt minion tear down", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
        List<GatewayConfig> liveGateways = allGatewayConfigs.stream()
                .filter(gw -> remainingIps.contains(gw.getPrivateAddress())).collect(Collectors.toList());
        for (GatewayConfig gatewayConfig : liveGateways) {
            try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig)) {
                sc.wheel("key.delete", removeNodePrivateIPsByFQDN.keySet(), Object.class);
                removeDeadSaltMinions(gatewayConfig);
            } catch (Exception e) {
                LOGGER.info("Error occurred during salt minion tear down", e);
                throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
            }
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public Map<String, Map<String, String>> getPackageVersionsFromAllHosts(GatewayConfig gateway, Map<String, Optional<String>> packages)
            throws CloudbreakOrchestratorFailedException {
        try (SaltConnector saltConnector = saltService.createSaltConnector(gateway)) {
            Map<String, List<PackageInfo>> packageVersions = saltStateService.getPackageVersions(saltConnector, packages);
            return packageVersions.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Entry::getKey, entry -> convertPackageInfoListToMap(entry.getValue())));
        } catch (RuntimeException e) {
            LOGGER.info("Error occurred during determine package versions: " + Joiner.on(",").join(packages.keySet()), e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public Map<String, List<PackageInfo>> getFullPackageVersionsFromAllHosts(GatewayConfig gateway, Map<String, Optional<String>> packages)
            throws CloudbreakOrchestratorFailedException {
        try (SaltConnector saltConnector = saltService.createSaltConnector(gateway)) {
            return saltStateService.getPackageVersions(saltConnector, packages);
        } catch (RuntimeException e) {
            LOGGER.info("Error occurred during determine package versions: " + Joiner.on(",").join(packages.keySet()), e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private Map<String, String> convertPackageInfoListToMap(List<PackageInfo> packageInfoList) {
        Map<String, String> versionMap = new HashMap<>();
        packageInfoList.forEach(packageInfo -> versionMap.put(packageInfo.getName(), packageInfo.getVersion()));
        return versionMap;
    }

    @Override
    public Map<String, String> runCommandOnAllHosts(GatewayConfig gateway, String command) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector saltConnector = saltService.createSaltConnector(gateway)) {
            return saltStateService.runCommand(retry, saltConnector, command);
        } catch (RuntimeException e) {
            LOGGER.info("Error occurred during command execution: " + command, e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> runCommandOnHosts(List<GatewayConfig> allGatewayConfigs, Set<String> targetFqdns, String command)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGatewayConfigs);
        Target<String> hosts = new HostList(targetFqdns);
        LOGGER.debug("Execute command: {}, on hosts: {}", command, hosts);
        try (SaltConnector saltConnector = saltService.createSaltConnector(primaryGateway)) {
            return saltStateService.runCommandOnHosts(retry, saltConnector, hosts, command);
        } catch (RuntimeException e) {
            LOGGER.warn("Error occurred during command execution: " + command, e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, JsonNode> getGrainOnAllHosts(GatewayConfig gateway, String grain) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector saltConnector = saltService.createSaltConnector(gateway)) {
            return saltStateService.getGrains(saltConnector, grain);
        } catch (RuntimeException e) {
            LOGGER.info("Error occurred during get grain execution: " + grain, e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void removeDeadSaltMinions(GatewayConfig gateway) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector saltConnector = saltService.createSaltConnector(gateway)) {
            MinionStatusSaltResponse minionStatusSaltResponse = saltStateService.collectNodeStatus(saltConnector);
            List<String> downNodes = minionStatusSaltResponse.downMinions();
            LOGGER.info("Deleting dead minions {} from {}", StringUtils.join(downNodes, ", "), gateway.getHostname());
            if (!CollectionUtils.isEmpty(downNodes)) {
                saltConnector.wheel("key.delete", downNodes, Object.class);
            }
        } catch (Exception e) {
            LOGGER.info("Error occurred during dead salt minions removal on " + gateway.getHostname(), e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> replacePatternInFileOnAllHosts(GatewayConfig gatewayConfig, String file, String pattern, String replace)
            throws CloudbreakOrchestratorFailedException {
        try (SaltConnector saltConnector = saltService.createSaltConnector(gatewayConfig)) {
            return saltStateService.replacePatternInFile(retry, saltConnector, file, pattern, replace);
        } catch (RuntimeException e) {
            LOGGER.warn("Error occurred during file replace execution in file '{}' while replacing pattern '{}' with '{}'", file, pattern, replace, e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void uploadRecipes(List<GatewayConfig> allGatewayConfigs, Map<String, List<RecipeModel>> recipes, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGatewayConfigs);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            OrchestratorBootstrap scriptPillarSave = PillarSave.createRecipesPillar(sc, gatewayTargets, recipes, calculateRecipeExecutionTimeout());
            Callable<Boolean> saltPillarRunner = saltRunner.runnerWithConfiguredErrorCount(scriptPillarSave, exitCriteria, exitModel);
            saltPillarRunner.call();

            for (List<RecipeModel> recipeList : recipes.values()) {
                for (RecipeModel model : recipeList) {
                    LOGGER.info("Uploading recipe with name [{}] and size: {} characters.", model.getName(), model.getGeneratedScript().length());
                    uploadRecipe(sc, gatewayTargets, exitModel, model.getName(), model.getGeneratedScript(), convert(model.getRecipeType()));
                }
            }
        } catch (Exception e) {
            LOGGER.info("Error occurred during recipe upload", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void uploadKeytabs(List<GatewayConfig> allGatewayConfigs, Set<KeytabModel> keytabModels, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGatewayConfig = saltService.getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGatewayConfigs);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGatewayConfig)) {
            Map<String, Object> properties = new HashMap<>();
            for (KeytabModel keytabModel : keytabModels) {
                uploadFileToTargets(sc, gatewayTargets, exitModel, keytabModel.getPath(), keytabModel.getFileName(), keytabModel.getKeytab());
                Map<String, String> keytabProps = Map.of(
                        "principal", keytabModel.getPrincipal(),
                        "path", keytabModel.getPath() + "/" + keytabModel.getFileName());
                properties.put(keytabModel.getService(), keytabProps);
            }
            SaltPillarProperties saltPillarProperties = new SaltPillarProperties("/kerberos/keytab.sls", Collections.singletonMap("keytab", properties));
            OrchestratorBootstrap pillarSave = PillarSave.createCustomPillar(sc, gatewayTargets, saltPillarProperties);
            Callable<Boolean> runner = saltRunner.runnerWithConfiguredErrorCount(pillarSave, exitCriteria, exitModel);
            runner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during keytab upload", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Map<String, String>> formatAndMountDisksOnNodesLegacy(List<GatewayConfig> gatewayConfigs, Set<Node> nodesWithDiskData,
            Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel, String platformVariant) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(gatewayConfigs);
        Set<String> allTargets = nodesWithDiskData.stream().map(Node::getHostname).collect(Collectors.toSet());
        Target<String> allHosts = new HostList(nodesWithDiskData.stream().map(Node::getHostname).collect(Collectors.toSet()));
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            uploadMountScriptsAndMakeThemExecutable(nodesWithDiskData, exitCriteriaModel, allTargets, allHosts, sc);

            saltStateService.runCommandOnHosts(retry, sc, allHosts, "(cd " + SRV_SALT_DISK + ";./" + DISK_INITIALIZE + ')');
            return nodesWithDiskData.stream()
                    .map(node -> {
                        Glob hostname = new Glob(node.getHostname());
                        String uuidList = formatDisks(platformVariant, sc, node, hostname);
                        Map<String, String> fstabResponse = mountDisks(platformVariant, sc, hostname, uuidList, node.getNodeVolumes().getFstab());
                        String fstab = fstabResponse.getOrDefault(node.getHostname(), "");
                        return new SimpleImmutableEntry<>(node.getHostname(), Map.of("uuids", uuidList, "fstab", fstab));
                    })
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        } catch (Exception e) {
            LOGGER.info("Error occurred during the salt bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getFreeDiskSpaceByNodes(Set<Node> nodes, List<GatewayConfig> gatewayConfigs) {
        Map<String, String> freeDiskSpaceByNode;
        try {
            GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(gatewayConfigs);
            SaltConnector sc = saltService.createSaltConnector(primaryGateway);
            Target<String> allHosts = new HostList(nodes.stream().map(Node::getHostname).collect(Collectors.toSet()));
            freeDiskSpaceByNode = saltStateService.runCommandOnHosts(retry, sc, allHosts, "df -k / | tail -1 | awk '{print $4}'");
        } catch (Exception e) {
            String errorMessage = String.format("Failed to get free disk space on hosts. Reason: %s", e.getMessage());
            LOGGER.warn(errorMessage, e);
            throw new CloudbreakServiceException(errorMessage, e);
        }
        return freeDiskSpaceByNode;
    }

    @Override
    public void applyDiagnosticsState(List<GatewayConfig> gatewayConfigs, String state, Map<String, Object> properties,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(gatewayConfigs);
        Target<String> gatewayHost = new HostList(Set.of(primaryGateway.getHostname()));
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            Map<String, Object> inlinePillars = Collections.singletonMap("filecollector", properties);
            saltStateService.applyState(sc, state, gatewayHost, inlinePillars);
        } catch (Exception e) {
            LOGGER.info("Error occurred during the salt bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private Long calculateRecipeExecutionTimeout() {
        return SLEEP_TIME_IN_SEC * (maxRetryRecipe - 2L);
    }

    @Override
    public byte[] getStateConfigZip() throws IOException {
        return compressUtil.generateCompressedOutputFromFolders("salt-common", "salt");
    }

    @Override
    public void preServiceDeploymentRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException {
        LOGGER.debug("Executing pre-service-deployment recipes.");
        executeRecipes(gatewayConfig, allNodes, exitCriteriaModel, RecipeExecutionPhase.PRE_SERVICE_DEPLOYMENT, false);
    }

    @Override
    public void postClusterManagerStartRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException {
        LOGGER.debug("Executing post-cloudera-manager-start recipes.");
        executeRecipes(gatewayConfig, allNodes, exitCriteriaModel, RecipeExecutionPhase.POST_CLOUDERA_MANAGER_START, false);
    }

    @Override
    public void postServiceDeploymentRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException {
        LOGGER.debug("Executing post-service-deployment recipes.");
        executeRecipes(gatewayConfig, allNodes, exitCriteriaModel, RecipeExecutionPhase.POST_SERVICE_DEPLOYMENT, false);
    }

    @Override
    public void preTerminationRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel, boolean forced)
            throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException {
        LOGGER.debug("Executing pre-termination recipes.");
        executeRecipes(gatewayConfig, allNodes, exitCriteriaModel, RecipeExecutionPhase.PRE_TERMINATION, forced);
    }

    @Override
    public void stopClusterManagerAgent(OrchestratorAware stack, GatewayConfig gatewayConfig, Set<Node> allNodes, Set<Node> nodesUnderStopping,
            ExitCriteriaModel exitCriteriaModel, CmAgentStopFlags flags) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig)) {
            NodeReachabilityResult nodeReachabilityResult = getResponsiveNodes(allNodes, sc, false);
            Set<Node> responsiveNodes = nodeReachabilityResult.getReachableNodes();
            Set<String> nodesUnderStoppingIPs = nodesUnderStopping.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            Set<Node> responsiveNodesUnderStopping = responsiveNodes.stream()
                    .filter(responsiveNode -> nodesUnderStoppingIPs.contains(responsiveNode.getPrivateIp())).collect(Collectors.toSet());
            if (!responsiveNodesUnderStopping.isEmpty()) {
                LOGGER.debug("Applying role 'cloudera_manager_agent_stop' on nodes: [{}]", responsiveNodesUnderStopping);
                Set<String> targetHostnames = responsiveNodesUnderStopping.stream().map(Node::getHostname).collect(Collectors.toSet());
                saltCommandRunner.runModifyGrainCommand(sc, new GrainAddRunner(saltStateService, targetHostnames, responsiveNodesUnderStopping, "roles",
                        "cloudera_manager_agent_stop"), exitCriteriaModel, exitCriteria);
                if (flags.isAdJoinable() || flags.isIpaJoinable()) {
                    String identityRole = flags.isAdJoinable() ? "ad_leave" : "ipa_leave";
                    LOGGER.debug("Applying role '{}' on nodes: [{}]", identityRole, responsiveNodesUnderStopping);
                    saltCommandRunner.runModifyGrainCommand(sc,
                            new GrainAddRunner(saltStateService, targetHostnames, responsiveNodesUnderStopping, "roles", identityRole),
                            exitCriteriaModel, exitCriteria);
                    String removeIdentityRole = flags.isAdJoinable() ? "ad_member" : "ipa_member";
                    LOGGER.debug("Removing role '{}' on nodes: [{}]", removeIdentityRole, responsiveNodesUnderStopping);
                    saltCommandRunner.runModifyGrainCommand(sc,
                            new GrainRemoveRunner(saltStateService, targetHostnames, responsiveNodesUnderStopping, "roles", removeIdentityRole),
                            exitCriteriaModel, exitCriteria);
                }

                Set<String> allHostnames = responsiveNodesUnderStopping.stream().map(Node::getHostname).collect(Collectors.toSet());
                runSyncAll(sc, allHostnames, responsiveNodesUnderStopping, exitCriteriaModel);

                saveHostsPillar(stack, exitCriteriaModel, getGatewayPrivateIps(Collections.singleton(gatewayConfig)), sc);
                runNewService(sc, new HighStateRunner(saltStateService, allHostnames, responsiveNodesUnderStopping), exitCriteriaModel, maxRetry, true);

                saltCommandRunner.runModifyGrainCommand(sc, new GrainRemoveRunner(saltStateService, targetHostnames, responsiveNodesUnderStopping, "roles",
                        "cloudera_manager_agent_stop"), exitCriteriaModel, exitCriteria);
                if (flags.isAdJoinable() || flags.isIpaJoinable()) {
                    String identityRole = flags.isAdJoinable() ? "ad_leave" : "ipa_leave";
                    saltCommandRunner.runModifyGrainCommand(sc,
                            new GrainRemoveRunner(saltStateService, targetHostnames, responsiveNodesUnderStopping, "roles", identityRole),
                            exitCriteriaModel, exitCriteria);
                }
            }
        } catch (Exception e) {
            LOGGER.info("Error occurred during executing highstate (for cluster manager agent stop).", e);
            throwExceptionIfNotForced(flags.isForced(), e);
        }
    }

    private void runSyncAll(SaltConnector sc, Set<String> targetHostnames, Set<Node> allNode, ExitCriteriaModel exitCriteriaModel) throws Exception {
        SaltJobIdTracker syncAllTracker = new SaltJobIdTracker(saltStateService, sc, new SyncAllRunner(saltStateService, targetHostnames, allNode));
        saltRunner.runnerWithConfiguredErrorCount(syncAllTracker, exitCriteria, exitCriteriaModel).call();
    }

    @Override
    public void uploadGatewayPillar(List<GatewayConfig> allGatewayConfigs, Set<Node> allNodes, ExitCriteriaModel exitModel, SaltConfig saltConfig)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGatewayConfig = saltService.getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGatewayConfigs);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGatewayConfig)) {
            SaltPillarProperties gatewayPillarProperties = saltConfig.getServicePillarConfig().get("gateway");
            OrchestratorBootstrap gatewayPillarSave = PillarSave.createCustomPillar(sc, gatewayTargets, gatewayPillarProperties);
            Callable<Boolean> saltPillarRunner = saltRunner.runnerWithConfiguredErrorCount(gatewayPillarSave, exitCriteria, exitModel);
            saltPillarRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during gateway pillar upload", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private void throwExceptionIfNotForced(boolean forced, Exception e) throws CloudbreakOrchestratorFailedException {
        if (!forced) {
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    public void leaveDomain(GatewayConfig gatewayConfig, Set<Node> allNodes, String roleToRemove, String roleToAdd, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig)) {
            if (isChangingRolesNecessary(gatewayConfig, sc, roleToRemove)) {
                Set<String> targetHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
                saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(saltStateService, targetHostnames, allNodes, "roles", roleToAdd),
                        exitCriteriaModel, maxRetryLeave, exitCriteria);
                saltCommandRunner.runSaltCommand(sc, new GrainRemoveRunner(saltStateService, targetHostnames, allNodes, "roles", roleToRemove),
                        exitCriteriaModel, maxRetryLeave, exitCriteria);
                Set<String> allHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
                SaltJobIdTracker syncAllTracker = new SaltJobIdTracker(saltStateService, sc, new SyncAllRunner(saltStateService, allHostnames, allNodes));
                saltRunner.runnerWithCalculatedErrorCount(syncAllTracker, exitCriteria, exitCriteriaModel, maxRetryLeave).call();
                runNewService(sc, new HighStateAllRunner(saltStateService, allHostnames, allNodes), exitCriteriaModel, maxRetryLeave, true);
            }
        } catch (Exception e) {
            LOGGER.info("Error occurred during executing highstate (for recipes).", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private boolean isChangingRolesNecessary(GatewayConfig gatewayConfig, SaltConnector sc, String role) {
        return getMemberRoles(gatewayConfig, sc).contains(role);
    }

    private Set<String> getMemberRoles(GatewayConfig gatewayConfig, SaltConnector sc) {
        Map<String, JsonNode> roles = saltStateService.getGrains(sc, new HostList(List.of(gatewayConfig.getHostname())), "roles");
        return roles.values().stream().findFirst()
                .map(GrainsJsonPropertyUtil::getPropertySet)
                .orElse(Collections.emptySet());
    }

    @Override
    public List<String> getMissingNodes(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return new ArrayList<>();
    }

    @Override
    public NodeReachabilityResult getResponsiveNodes(Set<Node> nodes, GatewayConfig gatewayConfig, boolean targeted) {
        try (SaltConnector saltConnector = saltService.createSaltConnector(gatewayConfig)) {
            return getResponsiveNodes(nodes, saltConnector, targeted);
        }
    }

    @Override
    public boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig) {
        try (SaltConnector saltConnector = saltService.createSaltConnector(gatewayConfig)) {
            if (saltConnector.health().getStatusCode() == HttpStatus.OK.value()) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.info("Failed to connect to bootstrap app {}", e.getMessage());
        }
        return false;
    }

    @Override
    public String name() {
        return SALT;
    }

    @Override
    public Map<String, String> getMembers(GatewayConfig gatewayConfig, List<String> privateIps) throws CloudbreakOrchestratorException {
        try (SaltConnector saltConnector = saltService.createSaltConnector(gatewayConfig)) {
            return saltConnector.members(privateIps);
        }
    }

    @Override
    public void backupDatabase(GatewayConfig primaryGateway, Set<String> target, SaltConfig saltConfig, ExitCriteriaModel exitModel,
            int databaseMaxDurationInMin)
            throws CloudbreakOrchestratorFailedException {
        callBackupRestore(primaryGateway, target, saltConfig, exitModel, DATABASE_BACKUP, databaseMaxDurationInMin);
    }

    @Override
    public void restoreDatabase(GatewayConfig primaryGateway, Set<String> target, SaltConfig saltConfig, ExitCriteriaModel exitModel,
            int databaseMaxDurationInMin)
            throws CloudbreakOrchestratorFailedException {
        callBackupRestore(primaryGateway, target, saltConfig, exitModel, DATABASE_RESTORE, databaseMaxDurationInMin);
    }

    @Override
    public void runOrchestratorState(OrchestratorStateParams stateParams) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = saltService.createSaltConnector(stateParams.getPrimaryGatewayConfig())) {
            StateRunner stateRunner = createStateRunner(stateParams);
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(saltStateService, sc, stateRunner);
            Optional<OrchestratorStateRetryParams> stateRetryParams = stateParams.getStateRetryParams();
            Callable<Boolean> saltJobRunBootstrapRunner = stateRetryParams.isPresent() ?
                    saltRunner.runner(saltJobIdTracker, exitCriteria, stateParams.getExitCriteriaModel(), stateRetryParams.get()) :
                    saltRunner.runner(saltJobIdTracker, exitCriteria, stateParams.getExitCriteriaModel());
            saltJobRunBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.error(stateParams.getErrorMessage(), e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void runOrchestratorGrainRunner(OrchestratorGrainRunnerParams grainRunnerParams) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = saltService.createSaltConnector(grainRunnerParams.getPrimaryGatewayConfig())) {
            Set<String> targetHostNames = grainRunnerParams.getTargetHostNames();
            GrainOperation grainOperation = grainRunnerParams.getGrainOperation();
            Set<Node> allNodes = grainRunnerParams.getAllNodes();
            String key = grainRunnerParams.getKey();
            String value = grainRunnerParams.getValue();

            ModifyGrainBase runner;
            switch (grainOperation) {
                case ADD:
                    runner = new GrainAddRunner(saltStateService, targetHostNames, allNodes, key, value);
                    break;
                case REMOVE:
                    runner = new GrainRemoveRunner(saltStateService, targetHostNames, allNodes, key, value);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + grainOperation);
            }
            saltCommandRunner.runModifyGrainCommand(sc, runner, grainRunnerParams.getExitCriteriaModel(), exitCriteria);

        } catch (Exception e) {
            LOGGER.error("Exception during grain runner", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public boolean unboundClusterConfigPresentOnAnyNodes(GatewayConfig primaryGateway, Set<String> nodes) {
        SaltConnector saltConnector = saltService.createSaltConnector(primaryGateway);
        return saltStateService.unboundClusterConfigPresentOnAnyNodes(saltConnector, new HostList(nodes));
    }

    private StateRunner createStateRunner(OrchestratorStateParams stateParams) {
        if (stateParams.isParameterized()) {
            if (stateParams.isConcurrent()) {
                return new ConcurrentParameterizedStateRunner(saltStateService, stateParams.getTargetHostNames(),
                        stateParams.getState(), stateParams.getStateParams());
            } else {
                return new ParameterizedStateRunner(saltStateService, stateParams.getTargetHostNames(),
                        stateParams.getState(), stateParams.getStateParams());
            }
        } else {
            return new StateRunner(saltStateService, stateParams.getTargetHostNames(), stateParams.getState());
        }
    }

    private void callBackupRestore(GatewayConfig primaryGateway, Set<String> target, SaltConfig saltConfig,
            ExitCriteriaModel exitModel, String state, int databaseMaxDurationInMin) throws CloudbreakOrchestratorFailedException {
        int maxDatabaseDrRetry = databaseMaxDurationInMin == 0 ? maxDatabaseDrRetryDefault :
                databaseMaxDurationInMin * SECONDS_IN_MIN / DATABASE_DR_EACH_RETRY_IN_SEC;
        LOGGER.info("Calling salt orchestrator on database backup/restore with retry number: {}", maxDatabaseDrRetry);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            for (Entry<String, SaltPillarProperties> propertiesEntry : saltConfig.getServicePillarConfig().entrySet()) {
                OrchestratorBootstrap pillarSave =
                        PillarSave.createCustomPillar(sc, Sets.newHashSet(primaryGateway.getPrivateAddress()), propertiesEntry.getValue());
                Callable<Boolean> saltPillarRunner = saltRunner.runner(pillarSave, exitCriteria, exitModel, maxDatabaseDrRetry, maxDatabaseDrRetryOnError);
                saltPillarRunner.call();
            }

            StateRunner stateRunner = new StateRunner(saltStateService, target, state);
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(saltStateService, sc, stateRunner);
            Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runner(saltJobIdTracker, exitCriteria, exitModel,
                    maxDatabaseDrRetry, maxDatabaseDrRetryOnError);
            saltJobRunBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.error("Error occurred during database backup/restore", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private Set<String> getGatewayPrivateIps(Collection<GatewayConfig> allGatewayConfigs) {
        return allGatewayConfigs.stream().map(GatewayConfig::getPrivateAddress).collect(Collectors.toSet());
    }

    private Set<String> getGatewayHostnames(Collection<GatewayConfig> allGatewayConfigs) {
        return allGatewayConfigs.stream().map(GatewayConfig::getHostname).collect(Collectors.toSet());
    }

    private void runNewService(SaltConnector sc, BaseSaltJobRunner baseSaltJobRunner, ExitCriteriaModel exitCriteriaModel) throws Exception {
        runNewService(sc, baseSaltJobRunner, exitCriteriaModel, maxRetry, true);
    }

    private void runNewService(SaltConnector sc, BaseSaltJobRunner baseSaltJobRunner, ExitCriteriaModel exitCriteriaModel, int maxRetry, boolean retryOnFail)
            throws Exception {
        OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(saltStateService, sc, baseSaltJobRunner, retryOnFail);
        Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runnerWithCalculatedErrorCount(saltJobIdTracker, exitCriteria, exitCriteriaModel, maxRetry);
        saltJobRunBootstrapRunner.call();
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    private void executeRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel,
            RecipeExecutionPhase phase, boolean forced) throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException {
        int maxRetry = forced ? maxRetryRecipeForced : maxRetryRecipe;
        RecipeExecutionPhase executedPhase = phase;
        try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig)) {
            executedPhase = fallbackToOldRecipeExecutionPhaseIfNecessary(phase, gatewayConfig, sc);
            Set<String> targetHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
            addRecipeGrainToNodes(allNodes, exitCriteriaModel, maxRetry, executedPhase, sc, targetHostnames);
            Set<String> allHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
            runSyncAll(sc, allHostnames, allNodes, exitCriteriaModel);
            if (executedPhase == PRE_CLOUDERA_MANAGER_START || executedPhase == PRE_SERVICE_DEPLOYMENT) {
                // Execute highstate before recipe. Otherwise ipa domain names will not be resolvable in recipe scripts.
                runNewService(sc, new HighStateAllRunner(saltStateService, allHostnames, allNodes), exitCriteriaModel, maxRetryRecipe, true);
            } else {
                runStateRunnerForRecipe(saltStateService, exitCriteriaModel, executedPhase, maxRetry, sc, targetHostnames);
            }
        } catch (CloudbreakOrchestratorTimeoutException e) {
            LOGGER.info("Recipe execution timeout. {}", executedPhase, e);
            throw e;
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.info("Orchestration error occurred during execution of recipes.", e);
            throw e;
        } catch (Exception e) {
            LOGGER.info("Unknown error occurred during execution of recipes.", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        } finally {
            try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig)) {
                // remove 'recipe' grain from all nodes
                Set<String> targetHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
                saltCommandRunner.runModifyGrainCommand(sc, new GrainRemoveRunner(saltStateService, targetHostnames, allNodes, "recipes",
                        executedPhase.value()), exitCriteriaModel, exitCriteria);
            } catch (Exception e) {
                LOGGER.info("Error occurred during removing recipe roles.", e);
                throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
            }
        }
    }

    private void addRecipeGrainToNodes(Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel, int maxRetry, RecipeExecutionPhase executedPhase,
            SaltConnector sc, Set<String> targetHostnames) throws Exception {
        saltCommandRunner.runModifyGrainCommand(sc, new GrainAddRunner(saltStateService, targetHostnames, allNodes, "recipes", executedPhase.value()),
                exitCriteriaModel, exitCriteria);
    }

    private RecipeExecutionPhase fallbackToOldRecipeExecutionPhaseIfNecessary(RecipeExecutionPhase phase, GatewayConfig gatewayConfig, SaltConnector sc) {
        boolean phaseSlsExists = doesRecipePhaseSlsExist(saltStateService, gatewayConfig, phase, sc);
        if (!phaseSlsExists) {
            RecipeExecutionPhase oldRecipeExecutionPhase = phase.oldRecipeExecutionPhase();
            LOGGER.info("Salt state file for new recipe execution phase ({}) was not found, it is a cluster with old salt states, fallback to the old " +
                    "recipe execution phase: {}", phase, oldRecipeExecutionPhase);
            return oldRecipeExecutionPhase;
        } else {
            return phase;
        }
    }

    public boolean doesPhaseSlsExistWithTimeouts(GatewayConfig gatewayConfig, String stateSlsName, int connectTimeoutMs, int readTimeout)
            throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig, connectTimeoutMs, readTimeout)) {
            Target<String> gatewayTargets = new HostList(Set.of(gatewayConfig.getHostname()));
            return saltStateService.stateSlsExists(sc, gatewayTargets, stateSlsName);
        } catch (Exception e) {
            LOGGER.info("Error occurred during phase sls check (with timeout settings: connect timeout {} ms, read timeout {} ms)", connectTimeoutMs,
                    readTimeout, e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private boolean doesRecipePhaseSlsExist(SaltStateService saltStateService, GatewayConfig gatewayConfig, RecipeExecutionPhase phase, SaltConnector sc) {
        Target<String> gatewayTargets = new HostList(Set.of(gatewayConfig.getHostname()));
        return saltStateService.stateSlsExists(sc, gatewayTargets, "recipes." + phase.value());
    }

    private void runStateRunnerForRecipe(SaltStateService saltStateService, ExitCriteriaModel exitCriteriaModel, RecipeExecutionPhase phase,
            int maxRetry, SaltConnector sc, Set<String> targetHostnames) throws Exception {
        StateRunner stateAllRunner = new StateRunner(saltStateService, targetHostnames, "recipes." + phase.value());
        OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(saltStateService, sc, stateAllRunner);
        Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runnerWithCalculatedErrorCount(saltJobIdTracker, exitCriteria, exitCriteriaModel, maxRetry);
        saltJobRunBootstrapRunner.call();
    }

    private void uploadSaltConfig(SaltConnector saltConnector, Set<String> targets, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException, IOException {
        uploadSaltConfig(saltConnector, targets, null, exitCriteriaModel);
    }

    private void uploadSaltConfig(SaltConnector saltConnector, Set<String> targets, byte[] stateConfigZip, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException, IOException {
        byte[] byteArray;
        byteArray = stateConfigZip == null || stateConfigZip.length == 0 ? getStateConfigZip() : stateConfigZip;
        LOGGER.debug("Upload salt.zip to gateways");
        uploadFileToTargets(saltConnector, targets, exitCriteriaModel, "/srv", "salt.zip", byteArray);
    }

    private void uploadSignKey(SaltConnector saltConnector, GatewayConfig gateway, Set<String> gatewayTargets,
            Set<String> targets, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorFailedException {
        try {
            String saltSignPrivateKey = gateway.getSaltSignPrivateKey();
            if (!gatewayTargets.isEmpty() && saltSignPrivateKey != null) {
                LOGGER.debug("Upload master_sign.pem to gateways");
                byte[] privateKeyContent = saltSignPrivateKey.getBytes();
                uploadFileToTargets(saltConnector, gatewayTargets, exitCriteriaModel, "/etc/salt/pki/master", "master_sign.pem", privateKeyContent);
            }

            String saltSignPublicKey = gateway.getSaltSignPublicKey();
            if (!targets.isEmpty() && saltSignPublicKey != null) {
                byte[] publicKeyContent = saltSignPublicKey.getBytes();
                LOGGER.debug("Upload master_sign.pub to nodes: " + targets);
                uploadFileToTargets(saltConnector, targets, exitCriteriaModel, "/etc/salt/pki/minion", "master_sign.pub", publicKeyContent);
            }
        } catch (SecurityException se) {
            throw new CloudbreakOrchestratorFailedException("Failed to read salt sign key: " + se.getMessage());
        }
    }

    private void uploadRecipe(SaltConnector sc, Set<String> targets, ExitCriteriaModel exitModel,
            String name, String recipe, RecipeExecutionPhase phase) throws CloudbreakOrchestratorFailedException {
        byte[] recipeBytes = recipe.getBytes(StandardCharsets.UTF_8);
        LOGGER.debug("Upload '{}' recipe: {}", phase.value(), name);
        String folder = phase.isPreRecipe() ? "pre-recipes" : "post-recipes";
        uploadFileToTargetsWithPermission(sc, targets, exitModel, "/srv/salt/" + folder + "/scripts", name, recipeBytes);
    }

    private void uploadFileToTargets(SaltConnector saltConnector, Set<String> targets, ExitCriteriaModel exitCriteriaModel,
            String path, String fileName, byte[] content) throws CloudbreakOrchestratorFailedException {
        try {
            OrchestratorBootstrap saltUpload = new SaltUpload(saltConnector, targets, path, fileName, content);
            Callable<Boolean> saltUploadRunner = saltRunner.runnerWithConfiguredErrorCount(saltUpload, exitCriteria, exitCriteriaModel);
            saltUploadRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during file distribute to gateway nodes", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private void uploadFileToTargetsWithPermission(SaltConnector saltConnector, Set<String> targets, ExitCriteriaModel exitCriteriaModel,
            String path, String fileName, byte[] content) throws CloudbreakOrchestratorFailedException {
        try {
            OrchestratorBootstrap saltUpload = new SaltUploadWithPermission(saltConnector, targets, path, fileName, PERMISSION, content);
            Callable<Boolean> saltUploadRunner = saltRunner.runnerWithConfiguredErrorCount(saltUpload, exitCriteria, exitCriteriaModel);
            saltUploadRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during file distribute to gateway nodes", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private NodeReachabilityResult getResponsiveNodes(Set<Node> nodes, SaltConnector sc, boolean targeted) {
        Set<Node> responsiveNodes = new HashSet<>();
        Set<Node> unresponsiveNodes = new HashSet<>();
        Set<String> minionIpAddresses;
        if (targeted) {
            minionIpAddresses = saltStateService.collectMinionIpAddresses(Optional.of(nodes), retry, sc);
        } else {
            minionIpAddresses = saltStateService.collectMinionIpAddresses(Optional.empty(), retry, sc);
        }
        nodes.forEach(node -> {
            if (minionIpAddresses.contains(node.getPrivateIp())) {
                LOGGER.info("Salt-minion is responding on host: {}", node);
                responsiveNodes.add(node);
            } else {
                LOGGER.warn("Salt-minion is not responding on host: {}", node);
                unresponsiveNodes.add(node);
            }
        });
        return new NodeReachabilityResult(responsiveNodes, unresponsiveNodes);
    }

    @Override
    public void uploadStates(List<GatewayConfig> allGatewayConfigs, ExitCriteriaModel exitModel) throws CloudbreakOrchestratorException {
        LOGGER.debug("Start upload to gateways: {}", allGatewayConfigs);
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGatewayConfigs);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            uploadSaltConfig(sc, gatewayTargets, exitModel);
        } catch (Exception e) {
            LOGGER.info("Error occurred during the salt state upload", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
        LOGGER.debug("Upload state finished");
    }

    @Override
    public LocalDate getPasswordExpiryDate(List<GatewayConfig> allGatewayConfigs, String user) throws CloudbreakOrchestratorException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargets = getGatewayHostnames(allGatewayConfigs);
        LOGGER.info("Getting password expiry date for user {} using primary gateway {} on all gateways: {} ",
                user, primaryGateway.getPrivateAddress(), gatewayTargets);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            String command = String.format("chage -l %s | grep \"Password expires\" | cut -d \":\" -f2", user);
            Map<String, String> passwordExpiryDatesOnHosts = saltStateService.runCommandOnHosts(retry, sc, new HostList(gatewayTargets), command);
            return passwordExpiryDatesOnHosts.values().stream()
                    .map(String::trim)
                    .map(SaltOrchestrator::parseDateString)
                    .min(LocalDate::compareTo)
                    .orElseThrow(() -> new IllegalStateException("No password expiry date found for user " + user));
        } catch (Exception e) {
            LOGGER.info("Error occurred during the salt state upload", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private static LocalDate parseDateString(String dateString) {
        return CHAGE_DATE_NEVER.equals(dateString) ? LocalDate.MAX : LocalDate.parse(dateString, CHAGE_DATE_PATTERN);
    }

    @Override
    public void createCronForUserHomeCreation(List<GatewayConfig> gatewayConfigs, Set<String> targets, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        LOGGER.info("Create cron for user home creation job on possible targets: {}", targets);
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(gatewayConfigs);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            boolean createUserHomeCronStateExists = saltStateService.stateSlsExists(sc, new HostList(targets), CREATE_USER_HOME_CRON);
            if (createUserHomeCronStateExists) {
                StateRunner stateRunner = new StateRunner(saltStateService, targets, CREATE_USER_HOME_CRON);
                OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(saltStateService, sc, stateRunner);
                Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runnerWithConfiguredErrorCount(saltJobIdTracker, exitCriteria, exitModel);
                saltJobRunBootstrapRunner.call();
            } else {
                LOGGER.debug("{} state not exists, the related cron will be created during post cluster install recipe execution.", CREATE_USER_HOME_CRON);
            }
        } catch (Exception e) {
            LOGGER.warn("Creating cron for user home creation failed.", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, JsonNode>> applyOrchestratorState(OrchestratorStateParams stateParams) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = stateParams.getPrimaryGatewayConfig();
        Target<String> gatewayHost = new HostList(stateParams.getTargetHostNames());
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            ApplyResponse response = saltStateService.applyStateSync(sc, stateParams.getState(), gatewayHost);
            return (List<Map<String, JsonNode>>) response.getResult();
        } catch (Exception e) {
            LOGGER.info("Error occurred during the salt bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }
}
