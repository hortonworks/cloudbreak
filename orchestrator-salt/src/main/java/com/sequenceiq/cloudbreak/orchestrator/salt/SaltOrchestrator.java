package com.sequenceiq.cloudbreak.orchestrator.salt;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SALT;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.type.RecipeExecutionPhase;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound.CompoundType;
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
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.SyncGrainsRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class SaltOrchestrator implements HostOrchestrator {

    private static final int SLEEP_TIME = 10000;

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltOrchestrator.class);

    @Value("${cb.max.salt.new.service.retry:90}")
    private int maxRetry;

    @Value("${cb.max.salt.recipe.execution.retry:90}")
    private int maxRetryRecipe;

    @Value("${rest.debug}")
    private boolean restDebug;

    @Value("${cb.smartsense.configure:false}")
    private boolean configureSmartSense;

    private ParallelOrchestratorComponentRunner parallelOrchestratorComponentRunner;

    private ExitCriteria exitCriteria;

    @Override
    public void init(ParallelOrchestratorComponentRunner parallelOrchestratorComponentRunner, ExitCriteria exitCriteria) {
        this.parallelOrchestratorComponentRunner = parallelOrchestratorComponentRunner;
        this.exitCriteria = exitCriteria;
    }

    @Override
    public void bootstrap(List<GatewayConfig> allGatewayConfigs, Set<Node> targets, ExitCriteriaModel exitModel) throws CloudbreakOrchestratorException {
        LOGGER.info("Start SaltBootstrap on nodes: {}", targets);
        GatewayConfig primaryGateway = getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGatewayConfigs);
        try (SaltConnector sc = new SaltConnector(primaryGateway, restDebug)) {
            uploadSaltConfig(sc, gatewayTargets, exitModel);
            uploadSignKey(sc, primaryGateway, gatewayTargets, targets.stream().map(Node::getPrivateIp).collect(Collectors.toSet()), exitModel);
            SaltBootstrap saltBootstrap = new SaltBootstrap(sc, allGatewayConfigs, targets);
            Callable<Boolean> saltBootstrapRunner = runner(saltBootstrap, exitCriteria, exitModel);
            Future<Boolean> saltBootstrapRunnerFuture = parallelOrchestratorComponentRunner.submit(saltBootstrapRunner);
            saltBootstrapRunnerFuture.get();
        } catch (Exception e) {
            LOGGER.error("Error occurred during the salt bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
        LOGGER.info("SaltBootstrap finished");
    }

    @Override
    public void bootstrapNewNodes(List<GatewayConfig> allGatewayConfigs, Set<Node> targets, Set<Node> allNodes, byte[] stateConfigZip,
            ExitCriteriaModel exitModel) throws CloudbreakOrchestratorException {
        GatewayConfig primaryGateway = getPrimaryGatewayConfig(allGatewayConfigs);
        Set<String> gatewayTargets = allGatewayConfigs.stream().filter(gc -> targets.stream().anyMatch(n -> gc.getPrivateAddress().equals(n.getPrivateIp())))
                .map(GatewayConfig::getPrivateAddress).collect(Collectors.toSet());
        try (SaltConnector sc = new SaltConnector(primaryGateway, restDebug)) {
            if (!gatewayTargets.isEmpty()) {
                uploadSaltConfig(sc, gatewayTargets, stateConfigZip, exitModel);
            }
            uploadSignKey(sc, primaryGateway, gatewayTargets, targets.stream().map(Node::getPrivateIp).collect(Collectors.toSet()), exitModel);
            // if there is a new salt master then re-bootstrap all nodes
            Set<Node> nodes = gatewayTargets.isEmpty() ? targets : allNodes;
            SaltBootstrap saltBootstrap = new SaltBootstrap(sc, allGatewayConfigs, nodes);
            Callable<Boolean> saltBootstrapRunner = runner(saltBootstrap, exitCriteria, exitModel);
            Future<Boolean> saltBootstrapRunnerFuture = parallelOrchestratorComponentRunner.submit(saltBootstrapRunner);
            saltBootstrapRunnerFuture.get();
        } catch (Exception e) {
            LOGGER.error("Error occurred during salt upscale", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void runService(List<GatewayConfig> allGateway, Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorException {
        LOGGER.info("Run Services on nodes: {}", allNodes);
        GatewayConfig primaryGateway = getPrimaryGatewayConfig(allGateway);
        Set<String> gatewayTargets = getGatewayPrivateIps(allGateway);
        String ambariServerAddress = primaryGateway.getPrivateAddress();
        try (SaltConnector sc = new SaltConnector(primaryGateway, restDebug)) {
            PillarSave hostSave = new PillarSave(sc, gatewayTargets, allNodes);
            Callable<Boolean> saltPillarRunner = runner(hostSave, exitCriteria, exitModel);
            Future<Boolean> saltPillarRunnerFuture = parallelOrchestratorComponentRunner.submit(saltPillarRunner);
            saltPillarRunnerFuture.get();

            for (Entry<String, SaltPillarProperties> propertiesEntry : saltConfig.getServicePillarConfig().entrySet()) {
                PillarSave pillarSave = new PillarSave(sc, gatewayTargets, propertiesEntry.getValue());
                saltPillarRunner = runner(pillarSave, exitCriteria, exitModel);
                saltPillarRunnerFuture = parallelOrchestratorComponentRunner.submit(saltPillarRunner);
                saltPillarRunnerFuture.get();
            }

            Set<String> server = Sets.newHashSet(ambariServerAddress);
            Set<String> all = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());

            // knox
            if (primaryGateway.getKnoxGatewayEnabled()) {
                runSaltCommand(sc, new GrainAddRunner(gatewayTargets, allNodes, "gateway"), exitModel);
            }
            // ambari server
            runSaltCommand(sc, new GrainAddRunner(server, allNodes, "ambari_server"), exitModel);
            // ambari server standby
            Set<String> standbyServers = gatewayTargets.stream().filter(ip -> !server.contains(ip)).collect(Collectors.toSet());
            if (!standbyServers.isEmpty()) {
                runSaltCommand(sc, new GrainAddRunner(standbyServers, allNodes, "ambari_server_standby"), exitModel);
            }
            // ambari agent
            runSaltCommand(sc, new GrainAddRunner(all, allNodes, "ambari_agent"), exitModel);
            // kerberos
            if (saltConfig.getServicePillarConfig().containsKey("kerberos")) {
                runSaltCommand(sc, new GrainAddRunner(server, allNodes, "kerberos_server_master"), exitModel);
                if (!standbyServers.isEmpty()) {
                    runSaltCommand(sc, new GrainAddRunner(standbyServers, allNodes, "kerberos_server_slave"), exitModel);
                }
            }
            // smartsense
            if (configureSmartSense) {
                runSaltCommand(sc, new GrainAddRunner(gatewayTargets, allNodes, "smartsense"), exitModel);
                runSaltCommand(sc, new GrainAddRunner(all, allNodes, "smartsense_agent_update"), exitModel);
            }
            uploadGrains(allNodes, saltConfig.getGrainsProperties(), exitModel, sc);

            runSaltCommand(sc, new SyncGrainsRunner(all, allNodes), exitModel);
            runSaltCommand(sc, new MineUpdateRunner(gatewayTargets, allNodes), exitModel);
            runNewService(sc, new HighStateRunner(all, allNodes), exitModel);
        } catch (Exception e) {
            LOGGER.error("Error occurred during ambari bootstrap", e);
            if (e instanceof ExecutionException && e.getCause() instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) e.getCause();
            }
            throw new CloudbreakOrchestratorFailedException(e);
        }
        LOGGER.info("Run services on nodes finished: {}", allNodes);
    }

    private void uploadGrains(Set<Node> allNodes, Map<String, Map<String, String>> grainsProperties, ExitCriteriaModel exitModel, SaltConnector sc)
            throws ExecutionException, InterruptedException {
        if (!grainsProperties.isEmpty()) {
            for (Entry<String, Map<String, String>> hostGrains : grainsProperties.entrySet()) {
                for (Entry<String, String> hostGrain : hostGrains.getValue().entrySet()) {
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
        LOGGER.info("Change primary gateway: {}", formerGateway);
        String ambariServerAddress = newPrimaryGateway.getPrivateAddress();
        try (SaltConnector sc = new SaltConnector(newPrimaryGateway, restDebug)) {
            SaltStates.stopMinions(sc, Collections.singletonMap(formerGateway.getHostname(), formerGateway.getPrivateAddress()));

            // change ambari_server_standby role to ambari_server
            Set<String> server = Collections.singleton(ambariServerAddress);
            runSaltCommand(sc, new GrainAddRunner(server, allNodes, "ambari_server"), exitCriteriaModel);
            runSaltCommand(sc, new GrainRemoveRunner(server, allNodes, "roles", "ambari_server_standby", CompoundType.IP), exitCriteriaModel);
            // add ambari_server_standby role to the standby servers and remove ambari_server role from them.
            Set<String> standByServers = allGatewayConfigs.stream()
                    .filter(gwc -> !gwc.getHostname().equals(newPrimaryGateway.getHostname()) && !gwc.getHostname().equals(formerGateway.getHostname()))
                    .map(gwc -> gwc.getPrivateAddress()).collect(Collectors.toSet());
            runSaltCommand(sc, new GrainAddRunner(standByServers, allNodes, "ambari_server_standby"), exitCriteriaModel);
            runSaltCommand(sc, new GrainRemoveRunner(standByServers, allNodes, "roles", "ambari_server", CompoundType.IP), exitCriteriaModel);

            // remove minion key from all remaining gateway nodes
            for (GatewayConfig gatewayConfig : allGatewayConfigs) {
                if (!gatewayConfig.getHostname().equals(formerGateway.getHostname())) {
                    try (SaltConnector sc1 = new SaltConnector(gatewayConfig, restDebug)) {
                        LOGGER.info("Removing minion key '{}' from gateway '{}'", formerGateway.getHostname(), gatewayConfig.getHostname());
                        sc1.wheel("key.delete", Collections.singleton(formerGateway.getHostname()), Object.class);
                    } catch (Exception ex) {
                        LOGGER.error("Unsuccessful key removal from gateway: " + gatewayConfig.getHostname(), ex);
                    }
                }
            }

            // salt '*' state.highstate
            runNewService(sc, new HighStateRunner(server, allNodes), exitCriteriaModel);
        } catch (Exception e) {
            LOGGER.error("Error occurred during primary gateway change", e);
            if (e instanceof ExecutionException && e.getCause() instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) e.getCause();
            }
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void resetAmbari(GatewayConfig gatewayConfig, Set<String> target, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try (SaltConnector saltConnector = new SaltConnector(gatewayConfig, restDebug)) {
            BaseSaltJobRunner baseSaltJobRunner = new BaseSaltJobRunner(target, allNodes) {
                @Override
                public String submit(SaltConnector saltConnector) {
                    return SaltStates.ambariReset(saltConnector, new Compound(getTarget(), CompoundType.HOST));
                }
            };
            SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(saltConnector, baseSaltJobRunner);
            Callable<Boolean> saltJobRunBootstrapRunner = runner(saltJobIdTracker, exitCriteria, exitCriteriaModel);
            Future<Boolean> saltJobRunBootstrapFuture = parallelOrchestratorComponentRunner.submit(saltJobRunBootstrapRunner);
            saltJobRunBootstrapFuture.get();
        } catch (Exception e) {
            LOGGER.error("Error occurred during reset", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void upgradeAmbari(GatewayConfig gatewayConfig, Set<String> target, Set<Node> allNodes, SaltConfig pillarConfig,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        try (SaltConnector sc = new SaltConnector(gatewayConfig, restDebug)) {
            for (Entry<String, SaltPillarProperties> propertiesEntry : pillarConfig.getServicePillarConfig().entrySet()) {
                PillarSave pillarSave = new PillarSave(sc, Sets.newHashSet(gatewayConfig.getPrivateAddress()), propertiesEntry.getValue());
                Callable<Boolean> saltPillarRunner = runner(pillarSave, exitCriteria, exitCriteriaModel);
                Future<Boolean> saltPillarRunnerFuture = parallelOrchestratorComponentRunner.submit(saltPillarRunner);
                saltPillarRunnerFuture.get();
            }

            // add 'ambari_upgrade' role to all nodes
            Set<String> targets = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new GrainAddRunner(targets, allNodes, "roles", "ambari_upgrade", CompoundType.IP), exitCriteriaModel);

            Set<String> all = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new SyncGrainsRunner(all, allNodes), exitCriteriaModel);
            runNewService(sc, new HighStateRunner(all, allNodes), exitCriteriaModel, maxRetryRecipe, true);

            // remove 'ambari_upgrade' role from all nodes
            targets = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new GrainRemoveRunner(targets, allNodes, "roles", "ambari_upgrade", CompoundType.IP), exitCriteriaModel);
        } catch (Exception e) {
            LOGGER.error("Error occurred during ambari upgrade", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void tearDown(List<GatewayConfig> allGatewayConfigs, Map<String, String> privateIPsByFQDN) throws CloudbreakOrchestratorException {
        GatewayConfig primaryGateway = getPrimaryGatewayConfig(allGatewayConfigs);
        try (SaltConnector saltConnector = new SaltConnector(primaryGateway, restDebug)) {
            SaltStates.stopMinions(saltConnector, privateIPsByFQDN);
        } catch (Exception e) {
            LOGGER.error("Error occurred during salt minion tear down", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
        List<GatewayConfig> liveGateways = allGatewayConfigs.stream()
                .filter(gw -> !privateIPsByFQDN.values().contains(gw.getPrivateAddress())).collect(Collectors.toList());
        for (GatewayConfig gatewayConfig : liveGateways) {
            try (SaltConnector sc = new SaltConnector(gatewayConfig, restDebug)) {
                sc.wheel("key.delete", privateIPsByFQDN.keySet(), Object.class);
            } catch (Exception e) {
                LOGGER.error("Error occurred during salt minion tear down", e);
                throw new CloudbreakOrchestratorFailedException(e);
            }
        }
    }

    @Override
    public void uploadRecipes(List<GatewayConfig> allGatewayConfigs, Map<String, List<RecipeModel>> recipes, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGateway = allGatewayConfigs.stream().filter(GatewayConfig::isPrimary).findFirst().get();
        Set<String> gatewayTargets = getGatewayPrivateIps(allGatewayConfigs);
        try (SaltConnector sc = new SaltConnector(primaryGateway, restDebug)) {
            PillarSave scriptPillarSave = new PillarSave(sc, gatewayTargets, recipes);
            Callable<Boolean> saltPillarRunner = runner(scriptPillarSave, exitCriteria, exitModel);
            Future<Boolean> saltPillarRunnerFuture = parallelOrchestratorComponentRunner.submit(saltPillarRunner);
            saltPillarRunnerFuture.get();

            for (List<RecipeModel> recipeList : recipes.values()) {
                for (RecipeModel model : recipeList) {
                    uploadRecipe(sc, gatewayTargets, exitModel, model.getName(), model.getScript(), RecipeExecutionPhase.convert(model.getRecipeType()));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred during recipe upload", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public byte[] getStateConfigZip() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ZipOutputStream zout = new ZipOutputStream(baos)) {
                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
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
    public void preInstallRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException {
        executeRecipes(gatewayConfig, allNodes, exitCriteriaModel, RecipeExecutionPhase.PRE);
    }

    @Override
    public void postInstallRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException {
        executeRecipes(gatewayConfig, allNodes, exitCriteriaModel, RecipeExecutionPhase.POST);
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
        try (SaltConnector saltConnector = new SaltConnector(gatewayConfig, restDebug)) {
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
        try (SaltConnector saltConnector = new SaltConnector(gatewayConfig, restDebug)) {
            return saltConnector.members(privateIps);
        } catch (IOException e) {
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    private GatewayConfig getPrimaryGatewayConfig(List<GatewayConfig> allGatewayConfigs) throws CloudbreakOrchestratorFailedException {
        Optional<GatewayConfig> gatewayConfig = allGatewayConfigs.stream().filter(GatewayConfig::isPrimary).findFirst();
        if (gatewayConfig.isPresent()) {
            return gatewayConfig.get();
        }
        throw new CloudbreakOrchestratorFailedException("No primary gateway specified");
    }

    private Set<String> getGatewayPrivateIps(List<GatewayConfig> allGatewayConfigs) {
        return allGatewayConfigs.stream().map(GatewayConfig::getPrivateAddress).collect(Collectors.toSet());
    }

    private void runNewService(SaltConnector sc, BaseSaltJobRunner baseSaltJobRunner, ExitCriteriaModel exitCriteriaModel) throws ExecutionException,
            InterruptedException {
        runNewService(sc, baseSaltJobRunner, exitCriteriaModel, maxRetry, true);
    }

    private void runNewService(SaltConnector sc, BaseSaltJobRunner baseSaltJobRunner, ExitCriteriaModel exitCriteriaModel, int maxRetry, boolean retryOnFail)
            throws ExecutionException, InterruptedException {
        SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(sc, baseSaltJobRunner, retryOnFail);
        Callable<Boolean> saltJobRunBootstrapRunner = runner(saltJobIdTracker, exitCriteria, exitCriteriaModel, maxRetry);
        Future<Boolean> saltJobRunBootstrapFuture = parallelOrchestratorComponentRunner.submit(saltJobRunBootstrapRunner);
        saltJobRunBootstrapFuture.get();
    }

    private void runSaltCommand(SaltConnector sc, BaseSaltJobRunner baseSaltJobRunner, ExitCriteriaModel exitCriteriaModel) throws ExecutionException,
            InterruptedException {
        SaltCommandTracker saltCommandTracker = new SaltCommandTracker(sc, baseSaltJobRunner);
        Callable<Boolean> saltCommandRunBootstrapRunner = runner(saltCommandTracker, exitCriteria, exitCriteriaModel);
        Future<Boolean> saltCommandRunBootstrapFuture = parallelOrchestratorComponentRunner.submit(saltCommandRunBootstrapRunner);
        saltCommandRunBootstrapFuture.get();
    }

    private void executeRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel, RecipeExecutionPhase phase)
            throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = new SaltConnector(gatewayConfig, restDebug)) {
            // add 'recipe' grain to all nodes
            Set<String> targets = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new GrainAddRunner(targets, allNodes, "recipes", phase.value(), CompoundType.IP), exitCriteriaModel);

            Set<String> all = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new SyncGrainsRunner(all, allNodes), exitCriteriaModel);
            runNewService(sc, new HighStateRunner(all, allNodes), exitCriteriaModel, maxRetryRecipe, false);

            // remove 'recipe' grain from all nodes
            targets = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new GrainRemoveRunner(targets, allNodes, "recipes", phase.value(), CompoundType.IP), exitCriteriaModel);
        } catch (Exception e) {
            LOGGER.error("Error occurred during recipe execution", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    private Callable<Boolean> runner(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel) {
        return runner(bootstrap, exitCriteria, exitCriteriaModel, maxRetry);
    }

    private Callable<Boolean> runner(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel, int maxRetry) {
        return new OrchestratorBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap(), maxRetry, SLEEP_TIME);
    }

    private void uploadSaltConfig(SaltConnector saltConnector, Set<String> targets, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException, IOException {
        uploadSaltConfig(saltConnector, targets, null, exitCriteriaModel);
    }

    private void uploadSaltConfig(SaltConnector saltConnector, Set<String> targets, byte[] stateConfigZip, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException, IOException {
        byte[] byteArray;
        if (stateConfigZip == null || stateConfigZip.length == 0) {
            byteArray = getStateConfigZip();
        } else {
            byteArray = stateConfigZip;
        }
        LOGGER.info("Upload salt.zip to gateways");
        uploadFileToGateways(saltConnector, targets, exitCriteriaModel, "/srv", "salt.zip", byteArray);
    }

    private void uploadSignKey(SaltConnector saltConnector, GatewayConfig gateway, Set<String> gatewayTargets,
            Set<String> targets, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorFailedException {
        try {
            String saltSignPrivateKey = gateway.getSaltSignPrivateKey();
            if (!gatewayTargets.isEmpty() && saltSignPrivateKey != null) {
                LOGGER.info("Upload master_sign.pem to gateways");
                byte[] privateKeyContent = saltSignPrivateKey.getBytes();
                uploadFileToGateways(saltConnector, gatewayTargets, exitCriteriaModel, "/etc/salt/pki/master", "master_sign.pem", privateKeyContent);
            }

            String saltSignPublicKey = gateway.getSaltSignPublicKey();
            if (!targets.isEmpty() && saltSignPublicKey != null) {
                byte[] publicKeyContent = saltSignPublicKey.getBytes();
                LOGGER.info("Upload master_sign.pub to nodes: " + targets);
                uploadFileToGateways(saltConnector, targets, exitCriteriaModel, "/etc/salt/pki/minion", "master_sign.pub", publicKeyContent);
            }
        } catch (SecurityException se) {
            throw new CloudbreakOrchestratorFailedException("Failed to read salt sign key: " + se.getMessage());
        }
    }

    private void uploadRecipe(SaltConnector sc, Set<String> targets, ExitCriteriaModel exitModel,
            String name, String recipe, RecipeExecutionPhase phase) throws CloudbreakOrchestratorFailedException {
        byte[] recipeBytes = recipe.getBytes(StandardCharsets.UTF_8);
        LOGGER.info("Upload '{}' recipe: {}", phase.value(), name);
        uploadFileToGateways(sc, targets, exitModel, "/srv/salt/" + phase.value() + "-recipes/scripts", name, recipeBytes);
    }

    private void uploadFileToGateways(SaltConnector saltConnector, Set<String> targets, ExitCriteriaModel exitCriteriaModel,
            String path, String fileName, byte[] content) throws CloudbreakOrchestratorFailedException {
        try {
            SaltUpload saltUpload = new SaltUpload(saltConnector, targets, path, fileName, content);
            Callable<Boolean> saltUploadRunner = runner(saltUpload, exitCriteria, exitCriteriaModel);
            Future<Boolean> saltUploadRunnerFuture = parallelOrchestratorComponentRunner.submit(saltUploadRunner);
            saltUploadRunnerFuture.get();
        } catch (Exception e) {
            LOGGER.error("Error occurred during file distribute to gateway nodes", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }
}