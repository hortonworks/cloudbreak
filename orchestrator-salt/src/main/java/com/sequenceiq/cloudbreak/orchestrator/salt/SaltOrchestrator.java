package com.sequenceiq.cloudbreak.orchestrator.salt;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SALT;
import static com.sequenceiq.cloudbreak.common.type.RecipeExecutionPhase.convert;
import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspath;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.type.RecipeExecutionPhase;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorTimeoutException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.KeytabModel;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionIpAddressesResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusSaltResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.grain.GrainUploader;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.PillarSave;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobIdTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltUpload;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainAddRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainRemoveRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.HighStateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.MineUpdateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.SyncAllRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltCommandRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.util.CompressUtil;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class SaltOrchestrator implements HostOrchestrator {

    private static final int SLEEP_TIME = 10000;

    private static final int SLEEP_TIME_IN_SEC = SLEEP_TIME / 1000;

    private static final String DISK_INITIALIZE = "format-and-mount-initialize.sh";

    private static final String DISK_COMMON = "format-and-mount-common.sh";

    private static final String DISK_FORMAT = "find-device-and-format.sh";

    private static final String DISK_MOUNT = "mount-disks.sh";

    private static final String DISK_SCRIPT_PATH = "salt/bootstrapnodes/";

    private static final String SRV_SALT_DISK = "/srv/salt/disk";

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltOrchestrator.class);

    @Value("${cb.max.salt.new.service.retry}")
    private int maxRetry;

    @Value("${cb.max.salt.new.service.leave.retry}")
    private int maxRetryLeave;

    @Value("${cb.max.salt.new.service.retry.onerror}")
    private int maxRetryOnError;

    @Value("${cb.max.salt.recipe.execution.retry}")
    private int maxRetryRecipe;

    @Value("${cb.max.salt.recipe.execution.retry.forced:2}")
    private int maxRetryRecipeForced;

    @Value("${rest.debug}")
    private boolean restDebug;

    @Inject
    private SaltRunner saltRunner;

    @Inject
    private SaltCommandRunner saltCommandRunner;

    @Inject
    private GrainUploader grainUploader;

    private ExitCriteria exitCriteria;

    @Override
    public void init(ExitCriteria exitCriteria) {
        this.exitCriteria = exitCriteria;
    }

    @Override
    public void bootstrap(List<GatewayConfig> allGatewayConfigs, Set<Node> targets, BootstrapParams params,
            ExitCriteriaModel exitModel) throws CloudbreakOrchestratorException {
        LOGGER.debug("Start SaltBootstrap on nodes: {}", targets);
        GatewayConfig primaryGateway = getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGatewayConfigs);
        try (SaltConnector sc = createSaltConnector(primaryGateway)) {
            uploadSaltConfig(sc, gatewayTargets, exitModel);
            Set<String> allTargets = targets.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            uploadSignKey(sc, primaryGateway, gatewayTargets, allTargets, exitModel);
            OrchestratorBootstrap saltBootstrap = new SaltBootstrap(sc, allGatewayConfigs, targets, params);
            Callable<Boolean> saltBootstrapRunner = saltRunner.runner(saltBootstrap, exitCriteria, exitModel);
            saltBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during the salt bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
        LOGGER.debug("SaltBootstrap finished");
    }

    @Override
    public Map<String, Map<String, String>> formatAndMountDisksOnNodes(List<GatewayConfig> allGateway, Set<Node> nodes, Set<Node> allNodes,
            ExitCriteriaModel exitModel, String platformVariant) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = getPrimaryGatewayConfig(allGateway);
        Set<String> gatewayTargetIpAddresses = getGatewayPrivateIps(allGateway);
        Target<String> allHosts = new HostList(nodes.stream().map(Node::getHostname).collect(Collectors.toSet()));
        try (SaltConnector sc = createSaltConnector(primaryGateway)) {
            initializePillar(allNodes, exitModel, gatewayTargetIpAddresses, sc);
            Callable<Boolean> saltPillarRunner;

            Map<String, String> dataVolumeMap = nodes.stream().collect(Collectors.toMap(Node::getHostname, Node::getDataVolumes));
            Map<String, String> serialIdMap = nodes.stream().collect(Collectors.toMap(Node::getHostname, Node::getSerialIds));
            Map<String, String> fstabMap = nodes.stream().collect(Collectors.toMap(Node::getHostname, Node::getFstab));

            Map<String, Object> hostnameDiskMountMap = nodes.stream().map(Node::getHostname).collect(Collectors.toMap(hn -> hn, hn -> Map.of(
                    "attached_volume_name_list", dataVolumeMap.getOrDefault(hn, ""),
                    "attached_volume_serial_list", serialIdMap.getOrDefault(hn, ""),
                    "cloud_platform", platformVariant,
                    "previous_fstab", fstabMap.getOrDefault(hn, "")
            )));

            SaltPillarProperties mounDiskProperties = new SaltPillarProperties("/mount/disk.sls", Collections.singletonMap("mount_data", hostnameDiskMountMap));

            OrchestratorBootstrap pillarSave = new PillarSave(sc, gatewayTargetIpAddresses, mounDiskProperties);
            saltPillarRunner = saltRunner.runner(pillarSave, exitCriteria, exitModel);
            saltPillarRunner.call();

            saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(hostnameDiskMountMap.keySet(), allNodes, "mount_disks"), exitModel, exitCriteria);

            BaseSaltJobRunner baseSaltJobRunner = new BaseSaltJobRunner(gatewayTargetIpAddresses, allNodes) {
                @Override
                public String submit(SaltConnector saltConnector) {
                    return SaltStates.mountDisks(saltConnector);
                }
            };
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(sc, baseSaltJobRunner);
            Callable<Boolean> saltJobRunBootstrapRunner = saltRunner.runner(saltJobIdTracker, exitCriteria, exitModel);
            saltJobRunBootstrapRunner.call();

            Map<String, String> uuidResponse = SaltStates.getUuidList(sc);

            saltCommandRunner.runSaltCommand(sc, new GrainRemoveRunner(hostnameDiskMountMap.keySet(), allNodes, "mount_disks"), exitModel, exitCriteria);
            Map<String, String> fstabResponse = SaltStates.runCommandOnHosts(sc, allHosts, "cat /etc/fstab");
            return nodes.stream()
                    .map(node -> {
                        String fstab = fstabResponse.getOrDefault(node.getHostname(), "");
                        String uuidList = uuidResponse.getOrDefault(node.getHostname(), "");
                        return new SimpleImmutableEntry<>(node.getHostname(), Map.of("uuids", uuidList, "fstab", fstab));
                    })
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        } catch (Exception e) {
            LOGGER.info("Error occurred during the salt bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    private void initializePillar(Set<Node> allNodes, ExitCriteriaModel exitModel, Set<String> gatewayTargetIpAddresses, SaltConnector sc) throws Exception {
        OrchestratorBootstrap hostSave = new PillarSave(sc, gatewayTargetIpAddresses, allNodes);
        Callable<Boolean> saltPillarRunner = saltRunner.runner(hostSave, exitCriteria, exitModel);
        saltPillarRunner.call();
    }

    private Map<String, String> mountDisks(String platformVariant, SaltConnector sc, Glob hostname, String uuidList, String fstab) {
        String mountCommandParams = "CLOUD_PLATFORM='" + platformVariant + "' ATTACHED_VOLUME_UUID_LIST='" + uuidList + "' ";

        if (!StringUtils.isEmpty(fstab)) {
            mountCommandParams += "PREVIOUS_FSTAB='" + fstab + "' ";
        }
        SaltStates.runCommandOnHosts(sc, hostname, "(cd " + SRV_SALT_DISK + ';' + mountCommandParams + " ./" + DISK_MOUNT + ')');
        return StringUtils.isEmpty(uuidList) ? Map.of() : SaltStates.runCommandOnHosts(sc, hostname, "cat /etc/fstab");
    }

    private String formatDisks(String platformVariant, SaltConnector sc, Node node, Glob hostname) {
        if (!StringUtils.isEmpty(node.getFstab())) {
            return node.getUuids();
        }

        String dataVolumes = String.join(" ", node.getDataVolumes());
        String serialIds = String.join(" ", node.getSerialIds());
        String formatCommandParams = "CLOUD_PLATFORM='" + platformVariant
                + "' ATTACHED_VOLUME_NAME_LIST='" + dataVolumes
                + "' ATTACHED_VOLUME_SERIAL_LIST='" + serialIds + "' ";
        String command = "(cd " + SRV_SALT_DISK + ';' + formatCommandParams + "./" + DISK_FORMAT + ')';
        Map<String, String> formatResponse = SaltStates.runCommandOnHosts(sc, hostname, command);
        return formatResponse.get(node.getHostname());
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
                    SaltStates.runCommandOnHosts(sc, allHosts, "chmod 755 " + path);
                });
    }

    @Override
    public void bootstrapNewNodes(List<GatewayConfig> allGatewayConfigs, Set<Node> targets, Set<Node> allNodes, byte[] stateConfigZip, BootstrapParams params,
            ExitCriteriaModel exitModel) throws CloudbreakOrchestratorException {
        GatewayConfig primaryGateway = getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargets = allGatewayConfigs.stream().filter(gc -> targets.stream().anyMatch(n -> gc.getPrivateAddress().equals(n.getPrivateIp())))
                .map(GatewayConfig::getPrivateAddress).collect(Collectors.toSet());
        try (SaltConnector sc = createSaltConnector(primaryGateway)) {
            if (!gatewayTargets.isEmpty()) {
                uploadSaltConfig(sc, gatewayTargets, stateConfigZip, exitModel);
            }
            uploadSignKey(sc, primaryGateway, gatewayTargets, targets.stream().map(Node::getPrivateIp).collect(Collectors.toSet()), exitModel);
            // if there is a new salt master then re-bootstrap all nodes
            Set<Node> nodes = gatewayTargets.isEmpty() ? targets : allNodes;
            OrchestratorBootstrap saltBootstrap = new SaltBootstrap(sc, allGatewayConfigs, nodes, params);
            Callable<Boolean> saltBootstrapRunner = saltRunner.runner(saltBootstrap, exitCriteria, exitModel);
            saltBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during salt upscale", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void initServiceRun(List<GatewayConfig> allGateway, Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorException {
        GatewayConfig primaryGateway = getPrimaryGatewayConfig(allGateway);
        Set<String> gatewayTargetIpAddresses = getGatewayPrivateIps(allGateway);
        Set<String> gatewayTargetHostnames = getGatewayHostnames(allGateway);
        Set<String> serverHostname = Sets.newHashSet(primaryGateway.getHostname());
        Set<String> allNodeHostname = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        try (SaltConnector sc = createSaltConnector(primaryGateway)) {
            OrchestratorBootstrap hostSave = new PillarSave(sc, gatewayTargetIpAddresses, allNodes);
            Callable<Boolean> saltPillarRunner = saltRunner.runner(hostSave, exitCriteria, exitModel);
            saltPillarRunner.call();

            for (Entry<String, SaltPillarProperties> propertiesEntry : saltConfig.getServicePillarConfig().entrySet()) {
                OrchestratorBootstrap pillarSave = new PillarSave(sc, gatewayTargetIpAddresses, propertiesEntry.getValue());
                saltPillarRunner = saltRunner.runner(pillarSave, exitCriteria, exitModel);
                saltPillarRunner.call();
            }

            setAdMemberRoleIfNeeded(allNodes, saltConfig, exitModel, sc, allNodeHostname);
            setIpaMemberRoleIfNeeded(allNodes, saltConfig, exitModel, sc, allNodeHostname);

            // knox
            if (primaryGateway.getKnoxGatewayEnabled()) {
                saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(gatewayTargetHostnames, allNodes, "gateway"), exitModel, exitCriteria);
            }

            setPostgreRoleIfNeeded(allNodes, saltConfig, exitModel, sc, serverHostname);

            addClusterManagerRoles(allNodes, exitModel, sc, serverHostname, allNodeHostname);

            // kerberos
            if (saltConfig.getServicePillarConfig().containsKey("kerberos")) {
                saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(allNodeHostname, allNodes, "kerberized"), exitModel, exitCriteria);
            }
            grainUploader.uploadGrains(allNodes, saltConfig.getGrainsProperties(), exitModel, sc, exitCriteria);

            saltCommandRunner.runSaltCommand(sc, new SyncAllRunner(allNodeHostname, allNodes), exitModel, exitCriteria);
            saltCommandRunner.runSaltCommand(sc, new MineUpdateRunner(gatewayTargetHostnames, allNodes), exitModel, exitCriteria);
        } catch (ExecutionException e) {
            LOGGER.warn("Error occurred during bootstrap", e);
            if (e.getCause() instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) e.getCause();
            }
            throw new CloudbreakOrchestratorFailedException(e);

        } catch (Exception e) {
            LOGGER.warn("Error occurred during bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void initSaltConfig(List<GatewayConfig> allGateway, Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = getPrimaryGatewayConfig(allGateway);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGateway);
        try (SaltConnector sc = createSaltConnector(primaryGateway)) {
            OrchestratorBootstrap hostSave = new PillarSave(sc, gatewayTargets, allNodes);
            Callable<Boolean> saltPillarRunner = saltRunner.runner(hostSave, exitCriteria, exitModel);
            saltPillarRunner.call();

            for (Entry<String, SaltPillarProperties> propertiesEntry : saltConfig.getServicePillarConfig().entrySet()) {
                OrchestratorBootstrap pillarSave = new PillarSave(sc, gatewayTargets, propertiesEntry.getValue());
                saltPillarRunner = saltRunner.runner(pillarSave, exitCriteria, exitModel);
                saltPillarRunner.call();
            }
        } catch (ExecutionException e) {
            LOGGER.warn("Error occurred during bootstrap", e);
            if (e.getCause() instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) e.getCause();
            }
            throw new CloudbreakOrchestratorFailedException(e);

        } catch (Exception e) {
            LOGGER.warn("Error occurred during bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    private void addClusterManagerRoles(Set<Node> allNodes, ExitCriteriaModel exitModel,
            SaltConnector sc, Set<String> serverHostnames, Set<String> allNodeHostname) throws Exception {
        saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(allNodeHostname, allNodes, "manager_agent"), exitModel, exitCriteria);
        saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(serverHostnames, allNodes, "manager_server"), exitModel, exitCriteria);
    }

    private void setAdMemberRoleIfNeeded(Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel, SaltConnector sc, Set<String> allHostnames)
            throws Exception {
        if (saltConfig.getServicePillarConfig().containsKey("sssd-ad")) {
            saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(allHostnames, allNodes, "ad_member"), exitModel, exitCriteria);
        }
    }

    private void setIpaMemberRoleIfNeeded(Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel, SaltConnector sc, Set<String> allHostnames)
            throws Exception {
        if (saltConfig.getServicePillarConfig().containsKey("sssd-ipa")) {
            saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(allHostnames, allNodes, "ipa_member"), exitModel, exitCriteria);
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void runService(List<GatewayConfig> allGateway, Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorException {
        LOGGER.debug("Run Services on nodes: {}", allNodes);
        GatewayConfig primaryGateway = getPrimaryGatewayConfig(allGateway);
        try (SaltConnector sc = createSaltConnector(primaryGateway)) {
            getRolesBeforeHighstateMagicWithRetry(sc);
            Set<String> allHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
            runNewService(sc, new HighStateRunner(allHostnames, allNodes), exitModel);
        } catch (ExecutionException e) {
            LOGGER.info("Error occurred during bootstrap", e);
            if (e.getCause() instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) e.getCause();
            }
            throw new CloudbreakOrchestratorFailedException(e);
        } catch (Exception e) {
            LOGGER.info("Error occurred during bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
        LOGGER.debug("Run services on nodes finished: {}", allNodes);
    }

    @Retryable
    private void getRolesBeforeHighstateMagicWithRetry(SaltConnector sc) {
        // YARN/SALT MAGIC: If you remove 'get role grains' before highstate, then highstate can run with defective roles,
        // so it can happen that some roles will be missing on some nodes. Please do not delete only if you know what you are doing.
        Map<String, JsonNode> roles = SaltStates.getGrains(sc, "roles");
        LOGGER.info("Roles before highstate: " + roles);
    }

    private void setPostgreRoleIfNeeded(Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel, SaltConnector sc, Set<String> serverHostname)
            throws Exception {
        if (saltConfig.getServicePillarConfig().containsKey("postgresql-server")) {
            saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(serverHostname, allNodes, "postgresql_server"), exitModel, exitCriteria);
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
    public void installFreeIPA(GatewayConfig primaryGateway, List<GatewayConfig> allGatewayConfigs, Set<Node> allNodes,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        Set<String> primaryServerHostname = Collections.singleton(primaryGateway.getHostname());

        Set<String> replicaServersHostnames = allGatewayConfigs.stream()
                .filter(gwc -> !gwc.getHostname().equals(primaryGateway.getHostname()))
                .map(GatewayConfig::getHostname).collect(Collectors.toSet());

        try (SaltConnector sc = createSaltConnector(primaryGateway)) {
            LOGGER.debug("Set primary FreeIPA: {}", primaryServerHostname);
            saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(primaryServerHostname, allNodes, "freeipa_primary"), exitCriteriaModel, exitCriteria);
            runNewService(sc, new HighStateRunner(primaryServerHostname, allNodes), exitCriteriaModel);

            LOGGER.debug("Set replica FreeIPA: {}", replicaServersHostnames);
            saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(replicaServersHostnames, allNodes, "freeipa_replica"), exitCriteriaModel, exitCriteria);
            runNewService(sc, new HighStateRunner(replicaServersHostnames, allNodes), exitCriteriaModel);
        } catch (ExecutionException e) {
            LOGGER.warn("Error occurred during FreeIPA installation", e);
            if (e.getCause() instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) e.getCause();
            }
            throw new CloudbreakOrchestratorFailedException(e);
        } catch (Exception e) {
            LOGGER.warn("Error occurred during FreeIPA installation", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
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

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void upgradeClusterManager(GatewayConfig gatewayConfig, Set<String> target, Set<Node> allNodes, SaltConfig pillarConfig,
            ExitCriteriaModel exitCriteriaModel) {
        LOGGER.debug("Upgrade of cluster manager is not implemented, yet");
    }

    @Override
    public void tearDown(List<GatewayConfig> allGatewayConfigs, Map<String, String> removeNodePrivateIPsByFQDN,
            Set<Node> remainingNodes, ExitCriteriaModel exitModel) throws CloudbreakOrchestratorException {
        LOGGER.debug("Tear down hosts: {},", removeNodePrivateIPsByFQDN);
        LOGGER.debug("Gateway config for tear down: {}", allGatewayConfigs);
        GatewayConfig primaryGateway = getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargetIpAddresses = getGatewayPrivateIps(allGatewayConfigs);
        try (SaltConnector saltConnector = createSaltConnector(primaryGateway)) {
            SaltStates.stopMinions(saltConnector, removeNodePrivateIPsByFQDN);
            if (!CollectionUtils.isEmpty(remainingNodes)) {
                OrchestratorBootstrap hostSave = new PillarSave(saltConnector, gatewayTargetIpAddresses, remainingNodes);
                Callable<Boolean> saltPillarRunner = saltRunner.runner(hostSave, exitCriteria, exitModel);
                saltPillarRunner.call();
            }
        } catch (Exception e) {
            LOGGER.info("Error occurred during salt minion tear down", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
        List<GatewayConfig> liveGateways = allGatewayConfigs.stream()
                .filter(gw -> !removeNodePrivateIPsByFQDN.containsValue(gw.getPrivateAddress())).collect(Collectors.toList());
        for (GatewayConfig gatewayConfig : liveGateways) {
            try (SaltConnector sc = createSaltConnector(gatewayConfig)) {
                sc.wheel("key.delete", removeNodePrivateIPsByFQDN.keySet(), Object.class);
                removeDeadSaltMinions(gatewayConfig);
            } catch (Exception e) {
                LOGGER.info("Error occurred during salt minion tear down", e);
                throw new CloudbreakOrchestratorFailedException(e);
            }
        }
    }

    public Map<String, Map<String, String>> getPackageVersionsFromAllHosts(GatewayConfig gateway, Map<String, Optional<String>> packages)
            throws CloudbreakOrchestratorFailedException {
        try (SaltConnector saltConnector = createSaltConnector(gateway)) {
            return SaltStates.getPackageVersions(saltConnector, packages);
        } catch (RuntimeException e) {
            LOGGER.info("Error occurred during determine package versions: " + Joiner.on(",").join(packages.keySet()), e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    public Map<String, String> runCommandOnAllHosts(GatewayConfig gateway, String command) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector saltConnector = createSaltConnector(gateway)) {
            return SaltStates.runCommand(saltConnector, command);
        } catch (RuntimeException e) {
            LOGGER.info("Error occurred during command execution: " + command, e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public Map<String, JsonNode> getGrainOnAllHosts(GatewayConfig gateway, String grain) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector saltConnector = createSaltConnector(gateway)) {
            return SaltStates.getGrains(saltConnector, grain);
        } catch (RuntimeException e) {
            LOGGER.info("Error occurred during get grain execution: " + grain, e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    private void removeDeadSaltMinions(GatewayConfig gateway) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector saltConnector = createSaltConnector(gateway)) {
            MinionStatusSaltResponse minionStatusSaltResponse = SaltStates.collectNodeStatus(saltConnector);
            List<String> downNodes = minionStatusSaltResponse.downMinions();
            if (downNodes != null && !downNodes.isEmpty()) {
                saltConnector.wheel("key.delete", downNodes, Object.class);
            }
        } catch (Exception e) {
            LOGGER.info("Error occurred during dead salt minions removal", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void uploadRecipes(List<GatewayConfig> allGatewayConfigs, Map<String, List<RecipeModel>> recipes, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = allGatewayConfigs.stream()
                .filter(GatewayConfig::isPrimary)
                .findFirst()
                .orElseThrow(() -> new CloudbreakOrchestratorFailedException("Primary gateway not found"));
        Set<String> gatewayTargets = getGatewayPrivateIps(allGatewayConfigs);
        try (SaltConnector sc = createSaltConnector(primaryGateway)) {
            OrchestratorBootstrap scriptPillarSave = new PillarSave(sc, gatewayTargets, recipes, calculateRecipeExecutionTimeout());
            Callable<Boolean> saltPillarRunner = saltRunner.runner(scriptPillarSave, exitCriteria, exitModel);
            saltPillarRunner.call();

            for (List<RecipeModel> recipeList : recipes.values()) {
                for (RecipeModel model : recipeList) {
                    uploadRecipe(sc, gatewayTargets, exitModel, model.getName(), model.getGeneratedScript(), convert(model.getRecipeType()));
                }
            }
        } catch (Exception e) {
            LOGGER.info("Error occurred during recipe upload", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void uploadKeytabs(List<GatewayConfig> allGatewayConfigs, Set<KeytabModel> keytabModels, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGatewayConfig = getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGatewayConfigs);
        try (SaltConnector sc = createSaltConnector(primaryGatewayConfig)) {
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
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public Map<String, Map<String, String>> formatAndMountDisksOnNodesLegacy(List<GatewayConfig> gatewayConfigs, Set<Node> nodes, Set<Node> allNodes,
            ExitCriteriaModel exitCriteriaModel, String platformVariant) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = getPrimaryGatewayConfig(gatewayConfigs);
        Set<String> allTargets = nodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        Target<String> allHosts = new HostList(nodes.stream().map(Node::getHostname).collect(Collectors.toSet()));
        try (SaltConnector sc = createSaltConnector(primaryGateway)) {
            uploadMountScriptsAndMakeThemExecutable(nodes, exitCriteriaModel, allTargets, allHosts, sc);

            SaltStates.runCommandOnHosts(sc, allHosts, "(cd " + SRV_SALT_DISK + ";./" + DISK_INITIALIZE + ')');
            return nodes.stream()
                    .map(node -> {
                        Glob hostname = new Glob(node.getHostname());
                        String uuidList = formatDisks(platformVariant, sc, node, hostname);
                        Map<String, String> fstabResponse = mountDisks(platformVariant, sc, hostname, uuidList, node.getFstab());
                        String fstab = fstabResponse.getOrDefault(node.getHostname(), "");
                        return new SimpleImmutableEntry<>(node.getHostname(), Map.of("uuids", uuidList, "fstab", fstab));
                    })
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        } catch (Exception e) {
            LOGGER.info("Error occurred during the salt bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    private Long calculateRecipeExecutionTimeout() {
        return SLEEP_TIME_IN_SEC * (maxRetryRecipe - 2L);
    }

    @Override
    public byte[] getStateConfigZip() throws IOException {
        return CompressUtil.generateCompressedOutputFromFolders("salt-common", "salt");
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
    public void stopClusterManagerAgent(GatewayConfig gatewayConfig, Set<Node> nodes, ExitCriteriaModel exitCriteriaModel, boolean adJoinable,
            boolean ipaJoinable) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = createSaltConnector(gatewayConfig)) {
            Set<Node> responsiveNodes = getResponsiveNodes(nodes, sc);
            if (!responsiveNodes.isEmpty()) {
                LOGGER.debug("Applying role 'cloudera_manager_agent_stop' on nodes: [{}]", responsiveNodes);
                Set<String> targetHostnames = responsiveNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
                saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(targetHostnames, responsiveNodes, "roles", "cloudera_manager_agent_stop"),
                        exitCriteriaModel, exitCriteria);
                if (adJoinable || ipaJoinable) {
                    String identityRole = adJoinable ? "ad_leave" : "ipa_leave";
                    LOGGER.debug("Applying role '{}' on nodes: [{}]", identityRole, responsiveNodes);
                    saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(targetHostnames, responsiveNodes, "roles", identityRole), exitCriteriaModel,
                            exitCriteria);
                    String removeIdentityRole = adJoinable ? "ad_member" : "ipa_member";
                    LOGGER.debug("Removing role '{}' on nodes: [{}]", removeIdentityRole, responsiveNodes);
                    saltCommandRunner.runSaltCommand(sc, new GrainRemoveRunner(targetHostnames, responsiveNodes, "roles", removeIdentityRole),
                            exitCriteriaModel, exitCriteria);
                }

                Set<String> allHostnames = responsiveNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
                saltCommandRunner.runSaltCommand(sc, new SyncAllRunner(allHostnames, responsiveNodes), exitCriteriaModel, exitCriteria);
                runNewService(sc, new HighStateRunner(allHostnames, responsiveNodes), exitCriteriaModel, maxRetry, true);

                saltCommandRunner.runSaltCommand(sc, new GrainRemoveRunner(targetHostnames, responsiveNodes, "roles", "cloudera_manager_agent_stop"),
                        exitCriteriaModel, exitCriteria);
                if (adJoinable || ipaJoinable) {
                    String identityRole = adJoinable ? "ad_leave" : "ipa_leave";
                    saltCommandRunner.runSaltCommand(sc, new GrainRemoveRunner(targetHostnames, responsiveNodes, "roles", identityRole), exitCriteriaModel,
                            exitCriteria);
                }
            }
        } catch (Exception e) {
            LOGGER.info("Error occurred during executing highstate (for cluster manager agent stop).", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    public void leaveDomain(GatewayConfig gatewayConfig, Set<Node> allNodes, String roleToRemove, String roleToAdd, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = createSaltConnector(gatewayConfig)) {
            Set<String> targetHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
            saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(targetHostnames, allNodes, "roles", roleToAdd), exitCriteriaModel, maxRetryLeave,
                    exitCriteria);
            saltCommandRunner.runSaltCommand(sc, new GrainRemoveRunner(targetHostnames, allNodes, "roles", roleToRemove), exitCriteriaModel, maxRetryLeave,
                    exitCriteria);
            Set<String> allHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
            saltCommandRunner.runSaltCommand(sc, new SyncAllRunner(allHostnames, allNodes), exitCriteriaModel, maxRetryLeave, exitCriteria);
            runNewService(sc, new HighStateRunner(allHostnames, allNodes), exitCriteriaModel, maxRetryLeave, true);
        } catch (Exception e) {
            LOGGER.info("Error occurred during executing highstate (for recipes).", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
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
    public boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig) {
        try (SaltConnector saltConnector = createSaltConnector(gatewayConfig)) {
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
        try (SaltConnector saltConnector = createSaltConnector(gatewayConfig)) {
            return saltConnector.members(privateIps);
        }
    }

    private GatewayConfig getPrimaryGatewayConfig(List<GatewayConfig> allGatewayConfigs) throws CloudbreakOrchestratorFailedException {
        Optional<GatewayConfig> gatewayConfigOptional = allGatewayConfigs.stream().filter(GatewayConfig::isPrimary).findFirst();
        if (gatewayConfigOptional.isPresent()) {
            GatewayConfig gatewayConfig = gatewayConfigOptional.get();
            LOGGER.debug("Primary gateway: {},", gatewayConfig);
            return gatewayConfig;
        }
        throw new CloudbreakOrchestratorFailedException("No primary gateway specified");
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
        boolean postRecipe = phase.isPostRecipe();
        int maxRetry = forced ? maxRetryRecipeForced : maxRetryRecipe;
        try (SaltConnector sc = createSaltConnector(gatewayConfig)) {
            // add 'recipe' grain to all nodes
            Set<String> targetHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
            saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(targetHostnames, allNodes, "recipes", phase.value()), exitCriteriaModel, maxRetry,
                    exitCriteria);

            // Add Deprecated 'PRE/POST' recipe execution for backward compatibility (since version 2.2.0)
            if (postRecipe) {
                saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(targetHostnames, allNodes, "recipes", RecipeExecutionPhase.POST.value()),
                        exitCriteriaModel, maxRetry, exitCriteria);
            } else {
                saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(targetHostnames, allNodes, "recipes", RecipeExecutionPhase.PRE.value()),
                        exitCriteriaModel, maxRetry, exitCriteria);
            }

            Set<String> allHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
            saltCommandRunner.runSaltCommand(sc, new SyncAllRunner(allHostnames, allNodes), exitCriteriaModel, maxRetry, exitCriteria);
            runNewService(sc, new HighStateRunner(allHostnames, allNodes), exitCriteriaModel, maxRetryRecipe, true);
        } catch (CloudbreakOrchestratorTimeoutException e) {
            LOGGER.info("Recipe execution timeout. {}", phase, e);
            throw e;
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.info("Orchestration error occurred during executing highstate (for recipes).", e);
            throw e;
        } catch (Exception e) {
            LOGGER.info("Unknown error occurred during executing highstate (for recipes).", e);
            throw new CloudbreakOrchestratorFailedException(e);
        } finally {
            try (SaltConnector sc = new SaltConnector(gatewayConfig, restDebug)) {
                // remove 'recipe' grain from all nodes
                Set<String> targetHostnames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
                saltCommandRunner.runSaltCommand(sc, new GrainRemoveRunner(targetHostnames, allNodes, "recipes", phase.value()), exitCriteriaModel,
                        maxRetry, exitCriteria);

                // Remove Deprecated 'PRE/POST' recipe execution for backward compatibility (since version 2.2.0)
                if (postRecipe) {
                    saltCommandRunner.runSaltCommand(sc, new GrainRemoveRunner(
                            targetHostnames, allNodes, "recipes", RecipeExecutionPhase.POST.value()), exitCriteriaModel, maxRetry, exitCriteria);
                } else {
                    saltCommandRunner.runSaltCommand(sc, new GrainRemoveRunner(
                            targetHostnames, allNodes, "recipes", RecipeExecutionPhase.PRE.value()), exitCriteriaModel, maxRetry, exitCriteria);
                }
            } catch (Exception e) {
                LOGGER.info("Error occurred during removing recipe roles.", e);
                throw new CloudbreakOrchestratorFailedException(e);
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
        uploadFileToTargets(sc, targets, exitModel, "/srv/salt/" + folder + "/scripts", name, recipeBytes);
    }

    private void uploadFileToTargets(SaltConnector saltConnector, Set<String> targets, ExitCriteriaModel exitCriteriaModel,
            String path, String fileName, byte[] content) throws CloudbreakOrchestratorFailedException {
        try {
            OrchestratorBootstrap saltUpload = new SaltUpload(saltConnector, targets, path, fileName, content);
            Callable<Boolean> saltUploadRunner = saltRunner.runner(saltUpload, exitCriteria, exitCriteriaModel);
            saltUploadRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during file distribute to gateway nodes", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    private SaltConnector createSaltConnector(GatewayConfig gatewayConfig) {
        return new SaltConnector(gatewayConfig, restDebug);
    }

    private Set<Node> getResponsiveNodes(Set<Node> nodes, SaltConnector sc) {
        Set<Node> responsiveNodes = new HashSet<>();
        MinionIpAddressesResponse minionIpAddressesResponse = SaltStates.collectMinionIpAddresses(sc);
        if (minionIpAddressesResponse != null) {
            nodes.forEach(node -> {
                if (minionIpAddressesResponse.getAllIpAddresses().contains(node.getPrivateIp())) {
                    LOGGER.info("Salt-minion is not responding on host: {}, yet", node);
                    responsiveNodes.add(node);
                }
            });
        } else {
            LOGGER.info("Minions ip address collection returned null value");
        }
        return responsiveNodes;
    }
}
