package com.sequenceiq.cloudbreak.orchestrator.salt;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SALT;
import static com.sequenceiq.cloudbreak.common.type.RecipeExecutionPhase.PRE_CLOUDERA_MANAGER_START;
import static com.sequenceiq.cloudbreak.common.type.RecipeExecutionPhase.convert;
import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspath;
import static java.util.function.Predicate.not;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.KeytabModel;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeVolumes;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostAndRoleTarget;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusSaltResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.grain.GrainUploader;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.PillarSave;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltBootstrap;
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
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;
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

    private static final String FREEIPA_MASTER_ROLE = "freeipa_primary";

    private static final String FREEIPA_MASTER_REPLACEMENT_ROLE = "freeipa_primary_replacement";

    private static final String FREEIPA_REPLICA_ROLE = "freeipa_replica";

    private static final String DATABASE_BACKUP = "postgresql.disaster_recovery.backup";

    private static final String DATABASE_RESTORE = "postgresql.disaster_recovery.restore";

    private static final String CM_SERVER_RESTART = "cloudera.manager.restart";

    private static final String CM_AGENT_CERTDIR_PERMISSION = "cloudera.agent.agent-cert-permission";

    private static final String DISK_INITIALIZE = "format-and-mount-initialize.sh";

    private static final String DISK_COMMON = "format-and-mount-common.sh";

    private static final String DISK_FORMAT = "find-device-and-format.sh";

    private static final String DISK_MOUNT = "mount-disks.sh";

    private static final String DISK_SCRIPT_PATH = "salt/bootstrapnodes/";

    private static final String SRV_SALT_DISK = "/srv/salt/disk";

    private static final String PERMISSION = "0600";

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltOrchestrator.class);

    @Value("${cb.max.salt.new.service.retry}")
    private int maxRetry;

    @Value("${cb.max.salt.new.service.leave.retry}")
    private int maxRetryLeave;

    @Value("${cb.max.salt.recipe.execution.retry}")
    private int maxRetryRecipe;

    @Value("${cb.max.salt.recipe.execution.retry.forced:2}")
    private int maxRetryRecipeForced;

    @Value("${cb.max.salt.database.dr.retry:60}")
    private int maxDatabaseDrRetry;

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

    @Override
    public void bootstrap(List<GatewayConfig> allGatewayConfigs, Set<Node> targets, BootstrapParams params,
            ExitCriteriaModel exitModel) throws CloudbreakOrchestratorException {
        LOGGER.debug("Start SaltBootstrap on nodes: {}", targets);
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGatewayConfigs);
        List<SaltConnector> saltConnectors = saltService.createSaltConnector(allGatewayConfigs);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            uploadSaltConfig(sc, gatewayTargets, exitModel);
            Set<String> allTargets = targets.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            uploadSignKey(sc, primaryGateway, gatewayTargets, allTargets, exitModel);
            OrchestratorBootstrap saltBootstrap = new SaltBootstrap(sc, saltConnectors, allGatewayConfigs, targets, params);
            Callable<Boolean> saltBootstrapRunner = saltRunner.runner(saltBootstrap, exitCriteria, exitModel);
            saltBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during the salt bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        } finally {
            saltConnectors.forEach(SaltConnector::close);
        }
        LOGGER.debug("SaltBootstrap finished");
    }

    @Override
    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public Map<String, Map<String, String>> formatAndMountDisksOnNodes(List<GatewayConfig> allGateway, Set<Node> nodesWithDiskData, Set<Node> allNodes,
            ExitCriteriaModel exitModel, String platformVariant) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateway);
        Set<String> gatewayTargetIpAddresses = getGatewayPrivateIps(allGateway);
        Target<String> allHosts = new HostList(nodesWithDiskData.stream().map(Node::getHostname).collect(Collectors.toSet()));
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            initializePillar(allNodes, exitModel, gatewayTargetIpAddresses, sc);
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

            SaltPillarProperties mounDiskProperties = new SaltPillarProperties("/mount/disk.sls", Collections.singletonMap("mount_data", hostnameDiskMountMap));

            OrchestratorBootstrap pillarSave = new PillarSave(sc, gatewayTargetIpAddresses, mounDiskProperties);
            saltPillarRunner = saltRunner.runner(pillarSave, exitCriteria, exitModel);
            saltPillarRunner.call();

            saltCommandRunner.runModifyGrainCommand(sc, new GrainAddRunner(hostnameDiskMountMap.keySet(), allNodes, "mount_disks"), exitModel, exitCriteria);

            StateAllRunner stateAllRunner = new StateAllRunner(gatewayTargetIpAddresses, allNodes, "disks.format-and-mount");
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(sc, stateAllRunner);
            Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runner(saltJobIdTracker, exitCriteria, exitModel);
            saltJobRunBootstrapRunner.call();

            Map<String, String> uuidResponse = SaltStates.getUuidList(sc);

            saltCommandRunner.runModifyGrainCommand(sc,
                    new GrainRemoveRunner(hostnameDiskMountMap.keySet(), allNodes, "mount_disks"), exitModel, exitCriteria);
            Map<String, String> fstabResponse = SaltStates.runCommandOnHosts(retry, sc, allHosts, "cat /etc/fstab");
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

    private void initializePillar(Set<Node> allNodes, ExitCriteriaModel exitModel, Set<String> gatewayTargetIpAddresses, SaltConnector sc) throws Exception {
        OrchestratorBootstrap hostSave = new PillarSave(sc, gatewayTargetIpAddresses, allNodes);
        Callable<Boolean> saltPillarRunner = saltRunner.runner(hostSave, exitCriteria, exitModel);
        saltPillarRunner.call();
    }

    private Map<String, String> mountDisks(String platformVariant, SaltConnector sc, Glob hostname, String uuidList, String fstab) {
        String mountCommandParams = "CLOUD_PLATFORM='" + platformVariant + "' ATTACHED_VOLUME_UUID_LIST='" + uuidList + "' ";

        if (StringUtils.isNotEmpty(fstab)) {
            mountCommandParams += "PREVIOUS_FSTAB='" + fstab + "' ";
        }
        SaltStates.runCommandOnHosts(retry, sc, hostname, "(cd " + SRV_SALT_DISK + ';' + mountCommandParams + " ./" + DISK_MOUNT + ')');
        return StringUtils.isEmpty(uuidList) ? Map.of() : SaltStates.runCommandOnHosts(retry, sc, hostname, "cat /etc/fstab");
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
        Map<String, String> formatResponse = SaltStates.runCommandOnHosts(retry, sc, hostname, command);
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
                    SaltStates.runCommandOnHosts(retry, sc, allHosts, "chmod 755 " + path);
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
                uploadSaltConfig(sc, gatewayTargets, stateConfigZip, exitModel);
                params.setRestartNeeded(true);
            }
            uploadSignKey(sc, primaryGateway, gatewayTargets, targets.stream().map(Node::getPrivateIp).collect(Collectors.toSet()), exitModel);
            // if there is a new salt master then re-bootstrap all nodes
            Set<Node> nodes = gatewayTargets.isEmpty() ? targets : allNodes;
            OrchestratorBootstrap saltBootstrap = new SaltBootstrap(sc, saltConnectors, allGatewayConfigs, nodes, params);
            Callable<Boolean> saltBootstrapRunner = saltRunner.runner(saltBootstrap, exitCriteria, exitModel);
            saltBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during salt upscale", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        } finally {
            saltConnectors.forEach(SaltConnector::close);
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void initServiceRun(List<GatewayConfig> allGateway, Set<Node> allNodes, Set<Node> reachableNodes, SaltConfig saltConfig,
            ExitCriteriaModel exitModel, String cloudPlatform) throws CloudbreakOrchestratorException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateway);
        Set<String> gatewayTargetIpAddresses = getGatewayPrivateIps(allGateway);
        Set<String> gatewayTargetHostnames = getGatewayHostnames(allGateway);
        Set<String> serverHostname = Sets.newHashSet(primaryGateway.getHostname());
        Set<String> reachableHostnames = reachableNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            OrchestratorBootstrap hostSave = new PillarSave(sc, gatewayTargetIpAddresses, allNodes);
            Callable<Boolean> saltPillarRunner = saltRunner.runner(hostSave, exitCriteria, exitModel);
            saltPillarRunner.call();

            savePillars(saltConfig, exitModel, gatewayTargetIpAddresses, sc);

            setAdMemberRoleIfNeeded(allNodes, saltConfig, exitModel, sc, reachableHostnames);
            setIpaMemberRoleIfNeeded(allNodes, saltConfig, exitModel, sc, reachableHostnames);

            // knox
            if (primaryGateway.getKnoxGatewayEnabled()) {
                saltCommandRunner.runModifyGrainCommand(sc, new GrainAddRunner(gatewayTargetHostnames, allNodes, "gateway"), exitModel, exitCriteria);
            }

            setPostgreRoleIfNeeded(allNodes, saltConfig, exitModel, sc, serverHostname);

            addClusterManagerRoles(allNodes, exitModel, sc, serverHostname, reachableHostnames);

            // kerberos
            if (saltConfig.getServicePillarConfig().containsKey("kerberos")) {
                saltCommandRunner.runModifyGrainCommand(sc, new GrainAddRunner(reachableHostnames, allNodes, "kerberized"), exitModel, exitCriteria);
            }
            grainUploader.uploadGrains(allNodes, saltConfig.getGrainsProperties(), exitModel, sc, exitCriteria);

            runSyncAll(sc, reachableHostnames, allNodes, exitModel);
            saltCommandRunner.runSaltCommand(sc, new MineUpdateRunner(gatewayTargetHostnames, allNodes), exitModel, exitCriteria);
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

    private void savePillars(SaltConfig saltConfig, ExitCriteriaModel exitModel, Set<String> gatewayTargetIpAddresses, SaltConnector sc) throws Exception {
        for (Entry<String, SaltPillarProperties> propertiesEntry : saltConfig.getServicePillarConfig().entrySet()) {
            OrchestratorBootstrap pillarSave = new PillarSave(sc, gatewayTargetIpAddresses, propertiesEntry.getValue());
            Callable<Boolean> saltPillarRunner = saltRunner.runner(pillarSave, exitCriteria, exitModel);
            saltPillarRunner.call();
        }
    }

    @Override
    public void initSaltConfig(List<GatewayConfig> allGateway, Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(allGateway);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGateway);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            OrchestratorBootstrap hostSave = new PillarSave(sc, gatewayTargets, allNodes);
            Callable<Boolean> saltPillarRunner = saltRunner.runner(hostSave, exitCriteria, exitModel);
            saltPillarRunner.call();

            savePillars(saltConfig, exitModel, gatewayTargets, sc);
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
        saltCommandRunner.runModifyGrainCommand(sc, new GrainAddRunner(reachableHostnames, allNodes, "manager_agent"), exitModel, exitCriteria);
        saltCommandRunner.runModifyGrainCommand(sc, new GrainAddRunner(serverHostnames, allNodes, "manager_server"), exitModel, exitCriteria);
    }

    private void setAdMemberRoleIfNeeded(Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel, SaltConnector sc,
            Set<String> reachableHostnames) throws Exception {
        if (saltConfig.getServicePillarConfig().containsKey("sssd-ad")) {
            saltCommandRunner.runModifyGrainCommand(sc, new GrainAddRunner(reachableHostnames, allNodes, "ad_member"), exitModel, exitCriteria);
        }
    }

    private void setIpaMemberRoleIfNeeded(Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel, SaltConnector sc,
            Set<String> reachableHostnames) throws Exception {
        if (saltConfig.getServicePillarConfig().containsKey("sssd-ipa")) {
            saltCommandRunner.runModifyGrainCommand(sc, new GrainAddRunner(reachableHostnames, allNodes, "ipa_member"), exitModel, exitCriteria);
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
            runNewService(sc, new HighStateRunner(allHostnames, allNodes), exitModel);
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
            Map<String, JsonNode> roles = SaltStates.getGrains(sc, "roles");
            LOGGER.info("Roles before highstate: " + roles);
        } catch (RuntimeException e) {
            LOGGER.info("Can't get roles before highstate", e);
            throw new Retry.ActionFailedException("Can't get roles before highstate: " + e.getMessage());
        }
    }

    private void setPostgreRoleIfNeeded(Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel, SaltConnector sc, Set<String> serverHostname)
            throws Exception {
        if (saltConfig.getServicePillarConfig().containsKey("postgresql-server")) {
            saltCommandRunner.runModifyGrainCommand(sc, new GrainAddRunner(serverHostname, allNodes, "postgresql_server"), exitModel, exitCriteria);
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
            runValidation(sc, new StateAllRunner(targetHostnames, allNodes, "validation/cloud-storage-backup"), exitCriteriaModel);
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
            runNewService(sc, new HighStateRunner(Set.of(existingReplicaHostname), allNodes), exitCriteriaModel);
        }
    }

    private void installFreeIpaReplicas(SaltConnector sc, Set<String> newFreeIpaReplicaHostnames, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws Exception {
        LOGGER.debug("New Replica FreeIPAs: [{}]", newFreeIpaReplicaHostnames);
        if (!newFreeIpaReplicaHostnames.isEmpty()) {
            saltCommandRunner.runModifyGrainCommand(sc, new GrainAddRunner(newFreeIpaReplicaHostnames, allNodes, FREEIPA_REPLICA_ROLE), exitCriteriaModel,
                    exitCriteria);
            runNewService(sc, new HighStateRunner(newFreeIpaReplicaHostnames, allNodes), exitCriteriaModel);
        }
    }

    private Set<String> installFreeIpaPrimary(SaltConnector sc, GatewayConfig primaryGateway, Set<String> freeIpaMasterHostname, Set<String> unassignedHostnames,
            Set<String> existingFreeIpaReplicaHostnames, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel) throws Exception {
        Set<String> freeIpaMasterHostnames = new HashSet<>(freeIpaMasterHostname);
        if (!freeIpaMasterHostnames.isEmpty()) {
            LOGGER.debug("Existing primary FreeIPA: {}", freeIpaMasterHostnames);
        } else if (existingFreeIpaReplicaHostnames.isEmpty()) {
            selectPrimaryGwAsFreeIpaMasterForInitialDeployment(sc, primaryGateway, allNodes, exitCriteriaModel, freeIpaMasterHostnames);
        } else {
            selectNewMasterReplacement(sc, primaryGateway, unassignedHostnames, existingFreeIpaReplicaHostnames, allNodes, exitCriteriaModel,
                    freeIpaMasterHostnames);
        }
        runNewService(sc, new HighStateRunner(freeIpaMasterHostnames, allNodes), exitCriteriaModel);
        return freeIpaMasterHostnames;
    }

    private void selectPrimaryGwAsFreeIpaMasterForInitialDeployment(SaltConnector sc, GatewayConfig primaryGateway, Set<Node> allNodes,
            ExitCriteriaModel exitCriteriaModel, Set<String> freeIpaMasterHostnames) throws Exception {
        freeIpaMasterHostnames.add(primaryGateway.getHostname());
        LOGGER.debug("Initial primary FreeIPA: {}", freeIpaMasterHostnames);
        saltCommandRunner.runModifyGrainCommand(sc,
                new GrainAddRunner(freeIpaMasterHostnames, allNodes, FREEIPA_MASTER_ROLE), exitCriteriaModel, exitCriteria);
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
                new GrainAddRunner(freeIpaMasterHostnames, allNodes, FREEIPA_MASTER_REPLACEMENT_ROLE), exitCriteriaModel, exitCriteria);
        saltCommandRunner.runModifyGrainCommand(sc, new GrainRemoveRunner(freeIpaMasterHostnames, allNodes, FREEIPA_REPLICA_ROLE), exitCriteriaModel,
                exitCriteria);
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
                roles = SaltStates.getGrains(sc, new HostAndRoleTarget(roleToSearch, targetHosts), "roles");
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
    public void restartClusterManagerOnMaster(GatewayConfig gatewayConfig, Set<String> target, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig)) {
            StateRunner stateRunner = new StateRunner(target, allNodes, CM_SERVER_RESTART);
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(sc, stateRunner);
            Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runner(saltJobIdTracker, exitCriteria, exitCriteriaModel);
            saltJobRunBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.error("Error occurred during CM Server restart", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void updateAgentCertDirectoryPermission(GatewayConfig gatewayConfig, Set<String> target, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig)) {
            StateRunner stateRunner = new StateRunner(target, allNodes, CM_AGENT_CERTDIR_PERMISSION);
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(sc, stateRunner);
            Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runner(saltJobIdTracker, exitCriteria, exitCriteriaModel);
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
                OrchestratorBootstrap pillarSave = new PillarSave(sc, Sets.newHashSet(gatewayConfig.getPrivateAddress()), propertiesEntry.getValue());
                Callable<Boolean> saltPillarRunner = saltRunner.runner(pillarSave, exitCriteria, exitCriteriaModel);
                saltPillarRunner.call();
            }

            // add 'manager_upgrade' role to all nodes
            Set<String> targetHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
            saltCommandRunner.runModifyGrainCommand(sc, new GrainAddRunner(targetHostnames, allNodes, "roles", "manager_upgrade"),
                    exitCriteriaModel, exitCriteria);

            Set<String> allHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
            runSyncAll(sc, allHostnames, allNodes, exitCriteriaModel);
            runNewService(sc, new HighStateRunner(allHostnames, allNodes), exitCriteriaModel, maxRetry, true);

            // remove 'manager_upgrade' role from all nodes
            saltCommandRunner.runModifyGrainCommand(sc, new GrainRemoveRunner(targetHostnames, allNodes, "roles", "manager_upgrade"),
                    exitCriteriaModel, exitCriteria);
        } catch (Exception e) {
            LOGGER.error("Error occurred during Cloudera Manager upgrade", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    @Override
    public void tearDown(List<GatewayConfig> allGatewayConfigs, Map<String, String> removeNodePrivateIPsByFQDN,
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
            SaltStates.stopMinions(saltConnector, minionsToStop);
            if (!CollectionUtils.isEmpty(remainingNodes)) {
                OrchestratorBootstrap hostSave = new PillarSave(saltConnector, gatewayTargetIpAddresses, remainingNodes);
                Callable<Boolean> saltPillarRunner = saltRunner.runner(hostSave, exitCriteria, exitModel);
                saltPillarRunner.call();
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
            return SaltStates.getPackageVersions(saltConnector, packages);
        } catch (RuntimeException e) {
            LOGGER.info("Error occurred during determine package versions: " + Joiner.on(",").join(packages.keySet()), e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    public Map<String, String> runCommandOnAllHosts(GatewayConfig gateway, String command) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector saltConnector = saltService.createSaltConnector(gateway)) {
            return SaltStates.runCommand(retry, saltConnector, command);
        } catch (RuntimeException e) {
            LOGGER.info("Error occurred during command execution: " + command, e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, JsonNode> getGrainOnAllHosts(GatewayConfig gateway, String grain) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector saltConnector = saltService.createSaltConnector(gateway)) {
            return SaltStates.getGrains(saltConnector, grain);
        } catch (RuntimeException e) {
            LOGGER.info("Error occurred during get grain execution: " + grain, e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void removeDeadSaltMinions(GatewayConfig gateway) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector saltConnector = saltService.createSaltConnector(gateway)) {
            MinionStatusSaltResponse minionStatusSaltResponse = SaltStates.collectNodeStatus(saltConnector);
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
            return SaltStates.replacePatternInFile(retry, saltConnector, file, pattern, replace);
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
            OrchestratorBootstrap scriptPillarSave = new PillarSave(sc, gatewayTargets, recipes, calculateRecipeExecutionTimeout());
            Callable<Boolean> saltPillarRunner = saltRunner.runner(scriptPillarSave, exitCriteria, exitModel);
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
            OrchestratorBootstrap pillarSave = new PillarSave(sc, gatewayTargets, saltPillarProperties);
            Callable<Boolean> runner = saltRunner.runner(pillarSave, exitCriteria, exitModel);
            runner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during keytab upload", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Map<String, String>> formatAndMountDisksOnNodesLegacy(List<GatewayConfig> gatewayConfigs, Set<Node> nodesWithDiskData, Set<Node> allNodes,
            ExitCriteriaModel exitCriteriaModel, String platformVariant) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(gatewayConfigs);
        Set<String> allTargets = nodesWithDiskData.stream().map(Node::getHostname).collect(Collectors.toSet());
        Target<String> allHosts = new HostList(nodesWithDiskData.stream().map(Node::getHostname).collect(Collectors.toSet()));
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            uploadMountScriptsAndMakeThemExecutable(nodesWithDiskData, exitCriteriaModel, allTargets, allHosts, sc);

            SaltStates.runCommandOnHosts(retry, sc, allHosts, "(cd " + SRV_SALT_DISK + ";./" + DISK_INITIALIZE + ')');
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
            freeDiskSpaceByNode = SaltStates.runCommandOnHosts(retry, sc, allHosts, "df -k / | tail -1 | awk '{print $4}'");
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
            SaltStates.applyState(sc, state, gatewayHost, inlinePillars);
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
    public void preClusterManagerStartRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException {
        LOGGER.debug("Executing pre-cloudera-manager-start recipes.");
        executeRecipes(gatewayConfig, allNodes, exitCriteriaModel, RecipeExecutionPhase.PRE_CLOUDERA_MANAGER_START, false);
    }

    @Override
    public void postClusterManagerStartRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException {
        LOGGER.debug("Executing post-cloudera-manager-start recipes.");
        executeRecipes(gatewayConfig, allNodes, exitCriteriaModel, RecipeExecutionPhase.POST_CLOUDERA_MANAGER_START, false);
    }

    @Override
    public void postInstallRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException {
        LOGGER.debug("Executing post-cluster-install recipes.");
        executeRecipes(gatewayConfig, allNodes, exitCriteriaModel, RecipeExecutionPhase.POST_CLUSTER_INSTALL, false);
    }

    @Override
    public void preTerminationRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel, boolean forced)
            throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException {
        LOGGER.debug("Executing pre-termination recipes.");
        executeRecipes(gatewayConfig, allNodes, exitCriteriaModel, RecipeExecutionPhase.PRE_TERMINATION, forced);
    }

    @Override
    public void stopClusterManagerAgent(GatewayConfig gatewayConfig, Set<Node> allNodes, Set<Node> nodesUnderStopping, ExitCriteriaModel exitCriteriaModel,
            boolean adJoinable, boolean ipaJoinable, boolean forced) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig)) {
            Set<Node> responsiveNodes = getResponsiveNodes(allNodes, sc);
            Set<String> nodesUnderStoppingIPs = nodesUnderStopping.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            Set<Node> responsiveNodesUnderStopping =
                    responsiveNodes.stream().filter(responsiveNode -> nodesUnderStoppingIPs.contains(responsiveNode.getPrivateIp())).collect(Collectors.toSet());
            if (!responsiveNodesUnderStopping.isEmpty()) {
                LOGGER.debug("Applying role 'cloudera_manager_agent_stop' on nodes: [{}]", responsiveNodesUnderStopping);
                Set<String> targetHostnames = responsiveNodesUnderStopping.stream().map(Node::getHostname).collect(Collectors.toSet());
                saltCommandRunner.runModifyGrainCommand(sc, new GrainAddRunner(targetHostnames, responsiveNodesUnderStopping, "roles",
                        "cloudera_manager_agent_stop"), exitCriteriaModel, exitCriteria);
                if (adJoinable || ipaJoinable) {
                    String identityRole = adJoinable ? "ad_leave" : "ipa_leave";
                    LOGGER.debug("Applying role '{}' on nodes: [{}]", identityRole, responsiveNodesUnderStopping);
                    saltCommandRunner.runModifyGrainCommand(sc, new GrainAddRunner(targetHostnames, responsiveNodesUnderStopping, "roles", identityRole),
                            exitCriteriaModel, exitCriteria);
                    String removeIdentityRole = adJoinable ? "ad_member" : "ipa_member";
                    LOGGER.debug("Removing role '{}' on nodes: [{}]", removeIdentityRole, responsiveNodesUnderStopping);
                    saltCommandRunner.runModifyGrainCommand(sc, new GrainRemoveRunner(targetHostnames, responsiveNodesUnderStopping, "roles",
                            removeIdentityRole), exitCriteriaModel, exitCriteria);
                }

                Set<String> allHostnames = responsiveNodesUnderStopping.stream().map(Node::getHostname).collect(Collectors.toSet());
                runSyncAll(sc, allHostnames, responsiveNodesUnderStopping, exitCriteriaModel);

                refreshPillars(gatewayConfig, allNodes, exitCriteriaModel, sc);
                runNewService(sc, new HighStateAllRunner(allHostnames, responsiveNodesUnderStopping), exitCriteriaModel, maxRetry, true);

                saltCommandRunner.runModifyGrainCommand(sc, new GrainRemoveRunner(targetHostnames, responsiveNodesUnderStopping, "roles",
                        "cloudera_manager_agent_stop"), exitCriteriaModel, exitCriteria);
                if (adJoinable || ipaJoinable) {
                    String identityRole = adJoinable ? "ad_leave" : "ipa_leave";
                    saltCommandRunner.runModifyGrainCommand(sc,
                            new GrainRemoveRunner(targetHostnames, responsiveNodesUnderStopping, "roles", identityRole), exitCriteriaModel, exitCriteria);
                }
            }
        } catch (Exception e) {
            LOGGER.info("Error occurred during executing highstate (for cluster manager agent stop).", e);
            throwExceptionIfNotForced(forced, e);
        }
    }

    private void refreshPillars(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel, SaltConnector sc) throws Exception {
        PillarSave pillarSave = new PillarSave(sc, getGatewayPrivateIps(Collections.singleton(gatewayConfig)), allNodes);
        Callable<Boolean> saltPillarRunner = saltRunner.runner(pillarSave, exitCriteria, exitCriteriaModel, maxDatabaseDrRetry,
                maxDatabaseDrRetryOnError);
        saltPillarRunner.call();
    }

    private void runSyncAll(SaltConnector sc, Set<String> targetHostnames, Set<Node> allNode, ExitCriteriaModel exitCriteriaModel) throws Exception {
        SaltJobIdTracker syncAllTracker = new SaltJobIdTracker(sc, new SyncAllRunner(targetHostnames, allNode));
        saltRunner.runner(syncAllTracker, exitCriteria, exitCriteriaModel, maxRetry, true).call();
    }

    @Override
    public void uploadGatewayPillar(List<GatewayConfig> allGatewayConfigs, Set<Node> allNodes, ExitCriteriaModel exitModel, SaltConfig saltConfig)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGatewayConfig = saltService.getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGatewayConfigs);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGatewayConfig)) {
            SaltPillarProperties gatewayPillarProperties = saltConfig.getServicePillarConfig().get("gateway");
            OrchestratorBootstrap gatewayPillarSave = new PillarSave(sc, gatewayTargets, gatewayPillarProperties);
            Callable<Boolean> saltPillarRunner = saltRunner.runnerWithUsingErrorCount(gatewayPillarSave, exitCriteria, exitModel);
            saltPillarRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during gateway pillar upload for certificate renewal", e);
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
                saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(targetHostnames, allNodes, "roles", roleToAdd),
                        exitCriteriaModel, maxRetryLeave, exitCriteria);
                saltCommandRunner.runSaltCommand(sc, new GrainRemoveRunner(targetHostnames, allNodes, "roles", roleToRemove),
                        exitCriteriaModel, maxRetryLeave, exitCriteria);
                Set<String> allHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
                SaltJobIdTracker syncAllTracker = new SaltJobIdTracker(sc, new SyncAllRunner(allHostnames, allNodes));
                saltRunner.runner(syncAllTracker, exitCriteria, exitCriteriaModel, maxRetryLeave, false).call();
                runNewService(sc, new HighStateAllRunner(allHostnames, allNodes), exitCriteriaModel, maxRetryLeave, true);
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
        Map<String, JsonNode> roles = SaltStates.getGrains(sc, new HostList(List.of(gatewayConfig.getHostname())), "roles");
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
    public Set<Node> getResponsiveNodes(Set<Node> nodes, GatewayConfig gatewayConfig) {
        try (SaltConnector saltConnector = saltService.createSaltConnector(gatewayConfig)) {
            return getResponsiveNodes(nodes, saltConnector);
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
    public void backupDatabase(GatewayConfig primaryGateway, Set<String> target, Set<Node> allNodes, SaltConfig saltConfig,
            ExitCriteriaModel exitModel) throws CloudbreakOrchestratorFailedException {
        callBackupRestore(primaryGateway, target, allNodes, saltConfig, exitModel, DATABASE_BACKUP);
    }

    @Override
    public void restoreDatabase(GatewayConfig primaryGateway, Set<String> target, Set<Node> allNodes, SaltConfig saltConfig,
            ExitCriteriaModel exitModel) throws CloudbreakOrchestratorFailedException {
        callBackupRestore(primaryGateway, target, allNodes, saltConfig, exitModel, DATABASE_RESTORE);
    }

    @Override
    public void runOrchestratorState(OrchestratorStateParams stateParams) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = saltService.createSaltConnector(stateParams.getPrimaryGatewayConfig())) {
            StateRunner stateRunner = createStateRunner(stateParams);
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(sc, stateRunner);
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
                    runner = new GrainAddRunner(targetHostNames, allNodes, key, value);
                    break;
                case REMOVE:
                    runner = new GrainRemoveRunner(targetHostNames, allNodes, key, value);
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
        return SaltStates.unboundClusterConfigPresentOnAnyNodes(saltConnector, new HostList(nodes));
    }

    private StateRunner createStateRunner(OrchestratorStateParams stateParams) {
        if (stateParams.isParameterized()) {
            if (stateParams.isConcurrent()) {
                return new ConcurrentParameterizedStateRunner(stateParams.getTargetHostNames(), stateParams.getAllNodes(), stateParams.getState(),
                        stateParams.getStateParams());
            } else {
                return new ParameterizedStateRunner(stateParams.getTargetHostNames(), stateParams.getAllNodes(), stateParams.getState(),
                        stateParams.getStateParams());
            }
        } else {
            return new StateRunner(stateParams.getTargetHostNames(), stateParams.getAllNodes(), stateParams.getState());
        }
    }

    private void callBackupRestore(GatewayConfig primaryGateway, Set<String> target, Set<Node> allNodes, SaltConfig saltConfig,
            ExitCriteriaModel exitModel, String state) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            for (Entry<String, SaltPillarProperties> propertiesEntry : saltConfig.getServicePillarConfig().entrySet()) {
                OrchestratorBootstrap pillarSave = new PillarSave(sc, Sets.newHashSet(primaryGateway.getPrivateAddress()), propertiesEntry.getValue());
                Callable<Boolean> saltPillarRunner = saltRunner.runner(pillarSave, exitCriteria, exitModel, maxDatabaseDrRetry, maxDatabaseDrRetryOnError);
                saltPillarRunner.call();
            }

            StateRunner stateRunner = new StateRunner(target, allNodes, state);
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(sc, stateRunner);
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
        OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(sc, baseSaltJobRunner, retryOnFail);
        Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runner(saltJobIdTracker, exitCriteria, exitCriteriaModel, maxRetry, true);
        saltJobRunBootstrapRunner.call();
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    private void executeRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel, RecipeExecutionPhase phase, boolean forced)
            throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException {
        int maxRetry = forced ? maxRetryRecipeForced : maxRetryRecipe;
        try (SaltConnector sc = saltService.createSaltConnector(gatewayConfig)) {
            // add 'recipe' grain to all nodes
            Set<String> targetHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
            saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(targetHostnames, allNodes, "recipes", phase.value()), exitCriteriaModel, maxRetry,
                    exitCriteria);
            Set<String> allHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
            runSyncAll(sc, allHostnames, allNodes, exitCriteriaModel);
            if (phase == PRE_CLOUDERA_MANAGER_START) {
                // Execute highstate before recipe. Otherwise ipa domain names will not be resolvable in recipe scripts.
                runNewService(sc, new HighStateAllRunner(allHostnames, allNodes), exitCriteriaModel, maxRetryRecipe, true);
            } else {
                // Skip highstate and just execute other recipes for performace.
                StateRunner stateAllRunner = new StateRunner(targetHostnames, allNodes, "recipes." + phase.value());
                OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(sc, stateAllRunner);
                Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runner(saltJobIdTracker, exitCriteria, exitCriteriaModel, maxRetry, false);
                saltJobRunBootstrapRunner.call();
            }
        } catch (CloudbreakOrchestratorTimeoutException e) {
            LOGGER.info("Recipe execution timeout. {}", phase, e);
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
                saltCommandRunner.runSaltCommand(sc, new GrainRemoveRunner(targetHostnames, allNodes, "recipes", phase.value()), exitCriteriaModel,
                        maxRetry, exitCriteria);
            } catch (Exception e) {
                LOGGER.info("Error occurred during removing recipe roles.", e);
                throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
            }
        }
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
            Callable<Boolean> saltUploadRunner = saltRunner.runner(saltUpload, exitCriteria, exitCriteriaModel);
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
            Callable<Boolean> saltUploadRunner = saltRunner.runner(saltUpload, exitCriteria, exitCriteriaModel);
            saltUploadRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during file distribute to gateway nodes", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    private Set<Node> getResponsiveNodes(Set<Node> nodes, SaltConnector sc) {
        Set<Node> responsiveNodes = new HashSet<>();
        Set<String> minionIpAddresses = SaltStates.collectMinionIpAddresses(retry, sc);
        nodes.forEach(node -> {
            if (minionIpAddresses.contains(node.getPrivateIp())) {
                LOGGER.info("Salt-minion is responding on host: {}", node);
                responsiveNodes.add(node);
            } else {
                LOGGER.warn("Salt-minion is not responding on host: {}", node);
            }
        });
        return responsiveNodes;
    }
}
