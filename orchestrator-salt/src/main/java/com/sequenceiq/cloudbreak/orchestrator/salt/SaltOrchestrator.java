package com.sequenceiq.cloudbreak.orchestrator.salt;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SALT;
import static com.sequenceiq.cloudbreak.common.type.RecipeExecutionPhase.convert;
import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspath;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.type.RecipeExecutionPhase;
import com.sequenceiq.cloudbreak.ccm.endpoint.DirectServiceEndpointFinder;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointFinder;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointLookupException;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound.CompoundType;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusSaltResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.PillarSave;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltCommandTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobIdTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltUpload;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainAddRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainRemoveRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.HighStateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.MineUpdateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.SyncAllRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

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

    @Value("${cb.max.salt.new.service.retry:90}")
    private int maxRetry;

    @Value("${cb.max.salt.new.service.retry.onerror:20}")
    private int maxRetryOnError;

    @Value("${cb.max.salt.recipe.execution.retry:90}")
    private int maxRetryRecipe;

    @Value("${rest.debug}")
    private boolean restDebug;

    @Autowired(required = false)
    private ServiceEndpointFinder serviceEndpointFinder = new DirectServiceEndpointFinder();

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
            Callable<Boolean> saltBootstrapRunner = runner(saltBootstrap, exitCriteria, exitModel);
            saltBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during the salt bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
        LOGGER.debug("SaltBootstrap finished");
    }

    @Override
    public Map<String, Map<String, String>> formatAndMountDisksOnNodes(List<GatewayConfig> allGateway, Set<Node> nodes, ExitCriteriaModel exitModel,
            String platformVariant) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = getPrimaryGatewayConfig(allGateway);
        Set<String> allTargets = nodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
        Compound allHosts = new Compound(nodes.stream().map(Node::getHostname).collect(Collectors.toSet()), CompoundType.HOST);
        try (SaltConnector sc = createSaltConnector(primaryGateway)) {
            uploadMountScriptsAndMakeThemExecutable(nodes, exitModel, allTargets, allHosts, sc);

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

    private void uploadMountScriptsAndMakeThemExecutable(Set<Node> nodes, ExitCriteriaModel exitModel, Set<String> allTargets, Compound allHosts,
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
            Callable<Boolean> saltBootstrapRunner = runner(saltBootstrap, exitCriteria, exitModel);
            saltBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during salt upscale", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void initServiceRun(List<GatewayConfig> allGateway, Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel, boolean clouderaManager)
            throws CloudbreakOrchestratorException {
        GatewayConfig primaryGateway = getPrimaryGatewayConfig(allGateway);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGateway);
        String ambariServerAddress = primaryGateway.getPrivateAddress();
        try (SaltConnector sc = createSaltConnector(primaryGateway)) {
            OrchestratorBootstrap hostSave = new PillarSave(sc, gatewayTargets, allNodes);
            Callable<Boolean> saltPillarRunner = runner(hostSave, exitCriteria, exitModel);
            saltPillarRunner.call();

            for (Entry<String, SaltPillarProperties> propertiesEntry : saltConfig.getServicePillarConfig().entrySet()) {
                OrchestratorBootstrap pillarSave = new PillarSave(sc, gatewayTargets, propertiesEntry.getValue());
                saltPillarRunner = runner(pillarSave, exitCriteria, exitModel);
                saltPillarRunner.call();
            }

            Set<String> server = Sets.newHashSet(ambariServerAddress);
            Set<String> allNodeIP = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());

            setAdMemberRoleIfNeeded(allNodes, saltConfig, exitModel, sc, allNodeIP);
            setIpaMemberRoleIfNeeded(allNodes, saltConfig, exitModel, sc, allNodeIP);

            // knox
            if (primaryGateway.getKnoxGatewayEnabled()) {
                runSaltCommand(sc, new GrainAddRunner(gatewayTargets, allNodes, "gateway"), exitModel);
            }

            setPostgreRoleIfNeeded(allNodes, saltConfig, exitModel, sc, server);

            addClusterManagerRoles(allNodes, exitModel, gatewayTargets, sc, server, allNodeIP, clouderaManager);

            // kerberos
            if (saltConfig.getServicePillarConfig().containsKey("kerberos")) {
                runSaltCommand(sc, new GrainAddRunner(allNodeIP, allNodes, "kerberized"), exitModel);
            }
            uploadGrains(allNodes, saltConfig.getGrainsProperties(), exitModel, sc);

            runSaltCommand(sc, new SyncAllRunner(allNodeIP, allNodes), exitModel);
            runSaltCommand(sc, new MineUpdateRunner(gatewayTargets, allNodes), exitModel);
        } catch (ExecutionException e) {
            LOGGER.warn("Error occurred during ambari bootstrap", e);
            if (e.getCause() instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) e.getCause();
            }
            throw new CloudbreakOrchestratorFailedException(e);

        } catch (Exception e) {
            LOGGER.warn("Error occurred during ambari bootstrap", e);
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
            Callable<Boolean> saltPillarRunner = runner(hostSave, exitCriteria, exitModel);
            saltPillarRunner.call();

            for (Entry<String, SaltPillarProperties> propertiesEntry : saltConfig.getServicePillarConfig().entrySet()) {
                OrchestratorBootstrap pillarSave = new PillarSave(sc, gatewayTargets, propertiesEntry.getValue());
                saltPillarRunner = runner(pillarSave, exitCriteria, exitModel);
                saltPillarRunner.call();
            }
        } catch (ExecutionException e) {
            LOGGER.warn("Error occurred during ambari bootstrap", e);
            if (e.getCause() instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) e.getCause();
            }
            throw new CloudbreakOrchestratorFailedException(e);

        } catch (Exception e) {
            LOGGER.warn("Error occurred during ambari bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    private void addClusterManagerRoles(Set<Node> allNodes, ExitCriteriaModel exitModel, Set<String> gatewayTargets,
            SaltConnector sc, Set<String> server, Set<String> allNodeIP, boolean clouderaManager) throws Exception {
        if (clouderaManager) {
            runSaltCommand(sc, new GrainAddRunner(server, allNodes, "manager_server"), exitModel);
            runSaltCommand(sc, new GrainAddRunner(allNodeIP, allNodes, "manager_agent"), exitModel);
        } else {
            // ambari server
            runSaltCommand(sc, new GrainAddRunner(server, allNodes, "ambari_server_install"), exitModel);
            runSaltCommand(sc, new GrainAddRunner(server, allNodes, "ambari_server"), exitModel);
            // ambari server standby
            Set<String> standbyServers = gatewayTargets.stream().filter(ip -> !server.contains(ip)).collect(Collectors.toSet());
            if (!standbyServers.isEmpty()) {
                runSaltCommand(sc, new GrainAddRunner(standbyServers, allNodes, "ambari_server_install"), exitModel);
                runSaltCommand(sc, new GrainAddRunner(standbyServers, allNodes, "ambari_server_standby"), exitModel);
            }
            // ambari agent
            runSaltCommand(sc, new GrainAddRunner(allNodeIP, allNodes, "ambari_agent_install"), exitModel);
            runSaltCommand(sc, new GrainAddRunner(allNodeIP, allNodes, "ambari_agent"), exitModel);
        }
    }

    private void setAdMemberRoleIfNeeded(Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel, SaltConnector sc, Set<String> all)
            throws Exception {
        if (saltConfig.getServicePillarConfig().containsKey("sssd-ad")) {
            runSaltCommand(sc, new GrainAddRunner(all, allNodes, "ad_member"), exitModel);
        }
    }

    private void setIpaMemberRoleIfNeeded(Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel, SaltConnector sc, Set<String> all)
            throws Exception {
        if (saltConfig.getServicePillarConfig().containsKey("sssd-ipa")) {
            runSaltCommand(sc, new GrainAddRunner(all, allNodes, "ipa_member"), exitModel);
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void runService(List<GatewayConfig> allGateway, Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorException {
        LOGGER.debug("Run Services on nodes: {}", allNodes);
        GatewayConfig primaryGateway = getPrimaryGatewayConfig(allGateway);
        try (SaltConnector sc = createSaltConnector(primaryGateway)) {
            Set<String> all = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            // YARN/SALT MAGIC: If you remove 'get role grains' before highstate, then highstate can run with defective roles,
            // so it can happen that 'ambari_agent' role will be missing on some nodes. Please do not delete only if you know what you are doing.
            Map<String, JsonNode> roles = SaltStates.getGrains(sc, "roles");
            LOGGER.info("Roles before highstate: " + roles);
            runNewService(sc, new HighStateRunner(all, allNodes), exitModel);
        } catch (ExecutionException e) {
            LOGGER.info("Error occurred during ambari bootstrap", e);
            if (e.getCause() instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) e.getCause();
            }
            throw new CloudbreakOrchestratorFailedException(e);
        } catch (Exception e) {
            LOGGER.info("Error occurred during ambari bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
        LOGGER.debug("Run services on nodes finished: {}", allNodes);
    }

    private void setPostgreRoleIfNeeded(Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel, SaltConnector sc, Set<String> server)
            throws Exception {
        if (saltConfig.getServicePillarConfig().containsKey("postgresql-server")) {
            runSaltCommand(sc, new GrainAddRunner(server, allNodes, "postgresql_server"), exitModel);
        }
    }

    private void uploadGrains(Set<Node> allNodes, Map<String, Map<String, String>> grainsProperties, ExitCriteriaModel exitModel, SaltConnector sc)
            throws Exception {
        if (!grainsProperties.isEmpty()) {
            for (Entry<String, Map<String, String>> hostGrains : grainsProperties.entrySet()) {
                for (Entry<String, String> hostGrain : hostGrains.getValue().entrySet()) {
                    LOGGER.debug("upload grains to host: " + hostGrain);
                    runSaltCommand(sc, new GrainAddRunner(Collections.singleton(hostGrains.getKey()), allNodes, hostGrain.getKey(), hostGrain.getValue(),
                            CompoundType.IP), exitModel);
                }
            }
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void changePrimaryGateway(GatewayConfig formerGateway, GatewayConfig newPrimaryGateway, List<GatewayConfig> allGatewayConfigs, Set<Node> allNodes,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        LOGGER.debug("Change primary gateway: {}", formerGateway);
        String ambariServerAddress = newPrimaryGateway.getPrivateAddress();
        try (SaltConnector sc = createSaltConnector(newPrimaryGateway)) {
            SaltStates.stopMinions(sc, Collections.singletonMap(formerGateway.getHostname(), formerGateway.getPrivateAddress()));

            // change ambari_server_standby role to ambari_server
            Set<String> server = Collections.singleton(ambariServerAddress);
            runSaltCommand(sc, new GrainAddRunner(server, allNodes, "ambari_server"), exitCriteriaModel);
            runSaltCommand(sc, new GrainRemoveRunner(server, allNodes, "roles", "ambari_server_standby", CompoundType.IP), exitCriteriaModel);
            // add ambari_server_standby role to the standby servers and remove ambari_server role from them.
            Set<String> standByServers = allGatewayConfigs.stream()
                    .filter(gwc -> !gwc.getHostname().equals(newPrimaryGateway.getHostname()) && !gwc.getHostname().equals(formerGateway.getHostname()))
                    .map(GatewayConfig::getPrivateAddress).collect(Collectors.toSet());
            runSaltCommand(sc, new GrainAddRunner(standByServers, allNodes, "ambari_server_standby"), exitCriteriaModel);
            runSaltCommand(sc, new GrainRemoveRunner(standByServers, allNodes, "roles", "ambari_server", CompoundType.IP), exitCriteriaModel);

            // remove minion key from all remaining gateway nodes
            for (GatewayConfig gatewayConfig : allGatewayConfigs) {
                if (!gatewayConfig.getHostname().equals(formerGateway.getHostname())) {
                    try (SaltConnector sc1 = createSaltConnector(gatewayConfig)) {
                        LOGGER.debug("Removing minion key '{}' from gateway '{}'", formerGateway.getHostname(), gatewayConfig.getHostname());
                        sc1.wheel("key.delete", Collections.singleton(formerGateway.getHostname()), Object.class);
                    } catch (Exception ex) {
                        LOGGER.info("Unsuccessful key removal from gateway: " + gatewayConfig.getHostname(), ex);
                    }
                }
            }

            // salt '*' state.highstate
            runNewService(sc, new HighStateRunner(server, allNodes), exitCriteriaModel);
        } catch (ExecutionException e) {
            LOGGER.warn("Error occurred during primary gateway change", e);
            if (e.getCause() instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) e.getCause();
            }
            throw new CloudbreakOrchestratorFailedException(e);
        } catch (Exception e) {
            LOGGER.warn("Error occurred during primary gateway change", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void resetAmbari(GatewayConfig gatewayConfig, Set<String> target, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try (SaltConnector saltConnector = createSaltConnector(gatewayConfig)) {
            BaseSaltJobRunner baseSaltJobRunner = new BaseSaltJobRunner(target, allNodes) {
                @Override
                public String submit(SaltConnector saltConnector) {
                    return SaltStates.ambariReset(saltConnector, new Compound(getTarget(), CompoundType.HOST));
                }
            };
            OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(saltConnector, baseSaltJobRunner);
            Callable<Boolean> saltJobRunBootstrapRunner = runner(saltJobIdTracker, exitCriteria, exitCriteriaModel);
            saltJobRunBootstrapRunner.call();
        } catch (Exception e) {
            LOGGER.warn("Error occurred during reset", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void stopAmbariOnMaster(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try (SaltConnector saltConnector = createSaltConnector(gatewayConfig)) {
            Set<String> master = Set.of(gatewayConfig.getPrivateAddress());
            runSaltCommand(saltConnector, new GrainAddRunner(master, allNodes, "ambari_single_master_repair_stop"), exitCriteriaModel);
            runNewService(saltConnector, new HighStateRunner(master, allNodes), exitCriteriaModel);
        } catch (Exception e) {
            LOGGER.info("Error occurred during stopping ambari agent and server", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void startAmbariOnMaster(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try (SaltConnector saltConnector = createSaltConnector(gatewayConfig)) {
            Set<String> master = Set.of(gatewayConfig.getPrivateAddress());
            runSaltCommand(saltConnector, new GrainRemoveRunner(master, allNodes, "ambari_single_master_repair_stop"), exitCriteriaModel);
            runNewService(saltConnector, new HighStateRunner(master, allNodes), exitCriteriaModel);
        } catch (Exception e) {
            LOGGER.info("Error occurred during starting ambari agent and server", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void upgradeAmbari(GatewayConfig gatewayConfig, Set<String> target, Set<Node> allNodes, SaltConfig pillarConfig,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        try (SaltConnector sc = createSaltConnector(gatewayConfig)) {
            for (Entry<String, SaltPillarProperties> propertiesEntry : pillarConfig.getServicePillarConfig().entrySet()) {
                OrchestratorBootstrap pillarSave = new PillarSave(sc, Sets.newHashSet(gatewayConfig.getPrivateAddress()), propertiesEntry.getValue());
                Callable<Boolean> saltPillarRunner = runner(pillarSave, exitCriteria, exitCriteriaModel);
                saltPillarRunner.call();
            }

            // add 'ambari_upgrade' role to all nodes
            Set<String> targets = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new GrainAddRunner(targets, allNodes, "roles", "ambari_upgrade", CompoundType.IP), exitCriteriaModel);

            Set<String> all = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new SyncAllRunner(all, allNodes), exitCriteriaModel);
            runNewService(sc, new HighStateRunner(all, allNodes), exitCriteriaModel, maxRetry, true);

            // remove 'ambari_upgrade' role from all nodes
            targets = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new GrainRemoveRunner(targets, allNodes, "roles", "ambari_upgrade", CompoundType.IP), exitCriteriaModel);
        } catch (Exception e) {
            LOGGER.warn("Error occurred during ambari upgrade", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void tearDown(List<GatewayConfig> allGatewayConfigs, Map<String, String> privateIPsByFQDN) throws CloudbreakOrchestratorException {
        LOGGER.debug("Tear down hosts: {},", privateIPsByFQDN);
        LOGGER.debug("Gateway config for tear down: {}", allGatewayConfigs);
        GatewayConfig primaryGateway = getPrimaryGatewayConfig(allGatewayConfigs);
        try (SaltConnector saltConnector = createSaltConnector(primaryGateway)) {
            SaltStates.stopMinions(saltConnector, privateIPsByFQDN);
        } catch (Exception e) {
            LOGGER.info("Error occurred during salt minion tear down", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
        List<GatewayConfig> liveGateways = allGatewayConfigs.stream()
                .filter(gw -> !privateIPsByFQDN.values().contains(gw.getPrivateAddress())).collect(Collectors.toList());
        for (GatewayConfig gatewayConfig : liveGateways) {
            try (SaltConnector sc = createSaltConnector(gatewayConfig)) {
                sc.wheel("key.delete", privateIPsByFQDN.keySet(), Object.class);
                removeDeadSaltMinions(gatewayConfig);
            } catch (Exception e) {
                LOGGER.info("Error occurred during salt minion tear down", e);
                throw new CloudbreakOrchestratorFailedException(e);
            }
        }
    }

    public Map<String, Map<String, String>> getPackageVersionsFromAllHosts(GatewayConfig gateway, String... packages)
            throws CloudbreakOrchestratorFailedException {
        try (SaltConnector saltConnector = createSaltConnector(gateway)) {
            return SaltStates.getPackageVersions(saltConnector, packages);
        } catch (RuntimeException e) {
            LOGGER.info("Error occurred during determine package versions: " + Arrays.deepToString(packages), e);
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
            Callable<Boolean> saltPillarRunner = runner(scriptPillarSave, exitCriteria, exitModel);
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

    private Long calculateRecipeExecutionTimeout() {
        return SLEEP_TIME_IN_SEC * (maxRetryRecipe - 2L);
    }

    @Override
    public byte[] getStateConfigZip() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ZipOutputStream zout = new ZipOutputStream(baos)) {
                ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Map<String, List<Resource>> structure = new TreeMap<>();
                for (Resource resource : resolver.getResources("classpath*:salt/**")) {
                    String path = resource.getURL().getPath();
                    String dir = path.substring(path.indexOf("/salt") + "/salt".length(), path.lastIndexOf('/') + 1);
                    List<Resource> list = structure.get(dir);
                    if (list == null) {
                        list = new ArrayList<>();
                    }
                    structure.put(dir, list);
                    if (!path.endsWith("/")) {
                        list.add(resource);
                    }
                }
                for (Entry<String, List<Resource>> entry : structure.entrySet()) {
                    zout.putNextEntry(new ZipEntry(entry.getKey()));
                    for (Resource resource : entry.getValue()) {
                        LOGGER.debug("Zip salt entry: {}", resource.getFilename());
                        zout.putNextEntry(new ZipEntry(entry.getKey() + resource.getFilename()));
                        InputStream inputStream = resource.getInputStream();
                        byte[] bytes = IOUtils.toByteArray(inputStream);
                        zout.write(bytes);
                        zout.closeEntry();
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to zip salt configurations", e);
                throw new IOException("Failed to zip salt configurations", e);
            }
            return baos.toByteArray();
        }
    }

    @Override
    public void preAmbariStartRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("Executing pre-ambari-start recipes.");
        executeRecipes(gatewayConfig, allNodes, exitCriteriaModel, RecipeExecutionPhase.PRE_AMBARI_START);
    }

    @Override
    public void postAmbariStartRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("Executing post-ambari-start recipes.");
        executeRecipes(gatewayConfig, allNodes, exitCriteriaModel, RecipeExecutionPhase.POST_AMBARI_START);
    }

    @Override
    public void postInstallRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("Executing post-cluster-install recipes.");
        executeRecipes(gatewayConfig, allNodes, exitCriteriaModel, RecipeExecutionPhase.POST_CLUSTER_INSTALL);
    }

    @Override
    public void preTerminationRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("Executing pre-termination recipes.");
        executeRecipes(gatewayConfig, allNodes, exitCriteriaModel, RecipeExecutionPhase.PRE_TERMINATION);
    }

    @Override
    public void stopClusterManagerAgent(GatewayConfig gatewayConfig, Set<Node> nodes, ExitCriteriaModel exitCriteriaModel, boolean adJoinable,
            boolean ipaJoinable) throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = createSaltConnector(gatewayConfig)) {
            LOGGER.debug("Applying role 'cloudera_manager_agent_stop' on nodes: [{}]", nodes);
            Set<String> targets = nodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new GrainAddRunner(targets, nodes, "roles", "cloudera_manager_agent_stop", CompoundType.IP), exitCriteriaModel);
            if (adJoinable || ipaJoinable) {
                String identityRole = adJoinable ? "ad_leave" : "ipa_leave";
                LOGGER.debug("Applying role '{}' on nodes: [{}]", identityRole, nodes);
                runSaltCommand(sc, new GrainAddRunner(targets, nodes, "roles", identityRole, CompoundType.IP), exitCriteriaModel);
            }

            Set<String> all = nodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new SyncAllRunner(all, nodes), exitCriteriaModel);
            runNewService(sc, new HighStateRunner(all, nodes), exitCriteriaModel, maxRetry, true);

            // remove 'recipe' grain from all nodes
            targets = nodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new GrainRemoveRunner(targets, nodes, "roles", "cloudera_manager_agent_stop", CompoundType.IP), exitCriteriaModel);
            if (adJoinable || ipaJoinable) {
                String identityRole = adJoinable ? "ad_leave" : "ipa_leave";
                runSaltCommand(sc, new GrainRemoveRunner(targets, nodes, "roles", identityRole, CompoundType.IP), exitCriteriaModel);
            }
        } catch (Exception e) {
            LOGGER.info("Error occurred during executing highstate (for cluster manager agent stop).", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    public void leaveDomain(GatewayConfig gatewayConfig, Set<Node> allNodes, String roleToRemove, String roleToAdd, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = createSaltConnector(gatewayConfig)) {
            Set<String> targets = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new GrainAddRunner(targets, allNodes, "roles", roleToAdd, CompoundType.IP), exitCriteriaModel);
            runSaltCommand(sc, new GrainRemoveRunner(targets, allNodes, "roles", roleToRemove, CompoundType.IP), exitCriteriaModel);
            Set<String> all = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new SyncAllRunner(all, allNodes), exitCriteriaModel);
            runNewService(sc, new HighStateRunner(all, allNodes), exitCriteriaModel, maxRetry, true);
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

    private void runNewService(SaltConnector sc, BaseSaltJobRunner baseSaltJobRunner, ExitCriteriaModel exitCriteriaModel) throws Exception {
        runNewService(sc, baseSaltJobRunner, exitCriteriaModel, maxRetry, true);
    }

    private void runNewService(SaltConnector sc, BaseSaltJobRunner baseSaltJobRunner, ExitCriteriaModel exitCriteriaModel, int maxRetry, boolean retryOnFail)
            throws Exception {
        OrchestratorBootstrap saltJobIdTracker = new SaltJobIdTracker(sc, baseSaltJobRunner, retryOnFail);
        Callable<Boolean> saltJobRunBootstrapRunner = runner(saltJobIdTracker, exitCriteria, exitCriteriaModel, maxRetry, true);
        saltJobRunBootstrapRunner.call();
    }

    private void runSaltCommand(SaltConnector sc, BaseSaltJobRunner baseSaltJobRunner, ExitCriteriaModel exitCriteriaModel) throws Exception {
        OrchestratorBootstrap saltCommandTracker = new SaltCommandTracker(sc, baseSaltJobRunner);
        Callable<Boolean> saltCommandRunBootstrapRunner = runner(saltCommandTracker, exitCriteria, exitCriteriaModel);
        saltCommandRunBootstrapRunner.call();
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    private void executeRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel, RecipeExecutionPhase phase)
            throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = createSaltConnector(gatewayConfig)) {
            // add 'recipe' grain to all nodes
            Set<String> targets = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new GrainAddRunner(targets, allNodes, "recipes", phase.value(), CompoundType.IP), exitCriteriaModel);

            // Add Deprecated 'PRE/POST' recipe execution for backward compatibility (since version 2.2.0)
            boolean postRecipe = phase.isPostRecipe();
            if (postRecipe) {
                runSaltCommand(sc, new GrainAddRunner(targets, allNodes, "recipes", RecipeExecutionPhase.POST.value(), CompoundType.IP), exitCriteriaModel);
            } else {
                runSaltCommand(sc, new GrainAddRunner(targets, allNodes, "recipes", RecipeExecutionPhase.PRE.value(), CompoundType.IP), exitCriteriaModel);
            }

            Set<String> all = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new SyncAllRunner(all, allNodes), exitCriteriaModel);
            runNewService(sc, new HighStateRunner(all, allNodes), exitCriteriaModel, maxRetryRecipe, true);

            // remove 'recipe' grain from all nodes
            targets = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new GrainRemoveRunner(targets, allNodes, "recipes", phase.value(), CompoundType.IP), exitCriteriaModel);

            // Remove Deprecated 'PRE/POST' recipe execution for backward compatibility (since version 2.2.0)
            if (postRecipe) {
                runSaltCommand(sc, new GrainRemoveRunner(targets, allNodes, "recipes", RecipeExecutionPhase.POST.value(), CompoundType.IP), exitCriteriaModel);
            } else {
                runSaltCommand(sc, new GrainRemoveRunner(targets, allNodes, "recipes", RecipeExecutionPhase.PRE.value(), CompoundType.IP), exitCriteriaModel);
            }
        } catch (Exception e) {
            LOGGER.info("Error occurred during executing highstate (for recipes).", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    private Callable<Boolean> runner(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel) {
        return runner(bootstrap, exitCriteria, exitCriteriaModel, maxRetry, false);
    }

    private Callable<Boolean> runner(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel, int maxRetry,
            boolean usingErrorCount) {
        return new OrchestratorBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap(), maxRetry, SLEEP_TIME,
                usingErrorCount ? maxRetryOnError : maxRetry);
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
            Callable<Boolean> saltUploadRunner = runner(saltUpload, exitCriteria, exitCriteriaModel);
            saltUploadRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during file distribute to gateway nodes", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    private SaltConnector createSaltConnector(GatewayConfig gatewayConfig) {
        try {
            return new SaltConnector(gatewayConfig.getGatewayConfig(serviceEndpointFinder), restDebug);
        } catch (ServiceEndpointLookupException | InterruptedException e) {
            throw new RuntimeException("Cannot determine service endpoint for gateway: " + gatewayConfig, e);
        }
    }
}
