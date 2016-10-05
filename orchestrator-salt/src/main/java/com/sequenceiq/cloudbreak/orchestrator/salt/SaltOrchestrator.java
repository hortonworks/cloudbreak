package com.sequenceiq.cloudbreak.orchestrator.salt;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SALT;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.springframework.util.StringUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeExecutionPhase;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.PillarSave;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltCommandTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobIdTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainAddRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainRemoveRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.HighStateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.SyncGrainsRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

@Component
public class SaltOrchestrator implements HostOrchestrator {

    private static final int MAX_NODES = 5000;
    private static final int MAX_RETRY_COUNT = 90;
    private static final int SLEEP_TIME = 10000;

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltOrchestrator.class);

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cb.smartsense.configure:false}")
    private boolean configureSmartSense;

    @Value("${cb.host.discovery.custom.domain:}")
    private String customDomain;

    private ParallelOrchestratorComponentRunner parallelOrchestratorComponentRunner;
    private ExitCriteria exitCriteria;

    @Override
    public void init(ParallelOrchestratorComponentRunner parallelOrchestratorComponentRunner, ExitCriteria exitCriteria) {
        this.parallelOrchestratorComponentRunner = parallelOrchestratorComponentRunner;
        this.exitCriteria = exitCriteria;
    }

    @Override
    public void bootstrap(GatewayConfig gatewayConfig, Set<Node> targets, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        LOGGER.info("Start SaltBootstrap on nodes: {}", targets);
        try (SaltConnector sc = new SaltConnector(gatewayConfig, restDebug)) {
            uploadSaltConfig(sc);
            SaltBootstrap saltBootstrap = new SaltBootstrap(sc, gatewayConfig, targets);
            Callable<Boolean> saltBootstrapRunner = runner(saltBootstrap, exitCriteria, exitCriteriaModel);
            Future<Boolean> saltBootstrapRunnerFuture = getParallelOrchestratorComponentRunner().submit(saltBootstrapRunner);
            saltBootstrapRunnerFuture.get();

        } catch (Exception e) {
            LOGGER.error("Error occurred during the salt bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
        LOGGER.info("SaltBootstrap finished");
    }

    @Override
    public void bootstrapNewNodes(GatewayConfig gatewayConfig, Set<Node> targets, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        try (SaltConnector sc = new SaltConnector(gatewayConfig, restDebug)) {
            SaltBootstrap saltBootstrap = new SaltBootstrap(sc, gatewayConfig, targets);
            Callable<Boolean> saltBootstrapRunner = runner(saltBootstrap, exitCriteria, exitCriteriaModel);
            Future<Boolean> saltBootstrapRunnerFuture = getParallelOrchestratorComponentRunner().submit(saltBootstrapRunner);
            saltBootstrapRunnerFuture.get();
        } catch (Exception e) {
            LOGGER.error("Error occurred during salt upscale", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void runService(GatewayConfig gatewayConfig, Set<Node> allNodes, SaltPillarConfig pillarConfig,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        LOGGER.info("Run Services on nodes: {}", allNodes);
        try (SaltConnector sc = new SaltConnector(gatewayConfig, restDebug)) {
            PillarSave ambariServer = new PillarSave(sc, gatewayConfig.getPrivateAddress());
            Callable<Boolean> saltPillarRunner = runner(ambariServer, exitCriteria, exitCriteriaModel);
            Future<Boolean> saltPillarRunnerFuture = getParallelOrchestratorComponentRunner().submit(saltPillarRunner);
            saltPillarRunnerFuture.get();

            PillarSave hostSave = new PillarSave(sc, allNodes, !StringUtils.isEmpty(customDomain));
            saltPillarRunner = runner(hostSave, exitCriteria, exitCriteriaModel);
            saltPillarRunnerFuture = getParallelOrchestratorComponentRunner().submit(saltPillarRunner);
            saltPillarRunnerFuture.get();

            for (Map.Entry<String, SaltPillarProperties> propertiesEntry : pillarConfig.getServicePillarConfig().entrySet()) {
                PillarSave pillarSave = new PillarSave(sc, propertiesEntry.getValue());
                saltPillarRunner = runner(pillarSave, exitCriteria, exitCriteriaModel);
                saltPillarRunnerFuture = getParallelOrchestratorComponentRunner().submit(saltPillarRunner);
                saltPillarRunnerFuture.get();
            }

            Set<String> server = Sets.newHashSet(gatewayConfig.getPrivateAddress());
            Set<String> all = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());

            LOGGER.info("Pillar saved, starting to set up discovery...");
            //run discovery only
            runNewService(sc, new HighStateRunner(all, allNodes), exitCriteriaModel);

            LOGGER.info("Pillar saved, discovery has been set up with highstate");

            // ambari server
            runSaltCommand(sc, new GrainAddRunner(server, allNodes, "ambari_server"), exitCriteriaModel);
            // ambari agent
            runSaltCommand(sc, new GrainAddRunner(all, allNodes, "ambari_agent"), exitCriteriaModel);
            // kerberos
            if (pillarConfig.getServicePillarConfig().containsKey("kerberos")) {
                runSaltCommand(sc, new GrainAddRunner(server, allNodes, "kerberos_server"), exitCriteriaModel);
            }
            if (pillarConfig.getServicePillarConfig().containsKey("ldap")) {
                runSaltCommand(sc, new GrainAddRunner(server, allNodes, "knox_gateway"), exitCriteriaModel);
            }
            if (configureSmartSense) {
                runSaltCommand(sc, new GrainAddRunner(server, allNodes, "smartsense"), exitCriteriaModel);
            }
            runSaltCommand(sc, new SyncGrainsRunner(all, allNodes), exitCriteriaModel);
            runNewService(sc, new HighStateRunner(all, allNodes), exitCriteriaModel);
        } catch (Exception e) {
            LOGGER.error("Error occurred during ambari bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
        LOGGER.info("Run Servcies on nodes finished: {}", allNodes);
    }

    @Override
    public void resetAmbari(GatewayConfig gatewayConfig, Set<String> target, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try (SaltConnector saltConnector = new SaltConnector(gatewayConfig, restDebug)) {
            BaseSaltJobRunner baseSaltJobRunner = new BaseSaltJobRunner(target, allNodes) {
                @Override
                public String submit(SaltConnector saltConnector) {
                    return SaltStates.ambariReset(saltConnector, new Compound(getTarget(), Compound.CompoundType.HOST));
                }
            };
            SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(saltConnector, baseSaltJobRunner);
            Callable<Boolean> saltJobRunBootstrapRunner = runner(saltJobIdTracker, exitCriteria, exitCriteriaModel);
            Future<Boolean> saltJobRunBootstrapFuture = getParallelOrchestratorComponentRunner().submit(saltJobRunBootstrapRunner);
            saltJobRunBootstrapFuture.get();
        } catch (Exception e) {
            LOGGER.error("Error occurred during reset", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void tearDown(GatewayConfig gatewayConfig, List<String> hostnames) throws CloudbreakOrchestratorException {
        try (SaltConnector saltConnector = new SaltConnector(gatewayConfig, restDebug)) {
            SaltStates.removeMinions(saltConnector, hostnames);
        } catch (Exception e) {
            LOGGER.error("Error occurred during salt minion tear down", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void uploadRecipes(GatewayConfig gatewayConfig, Map<String, List<RecipeModel>> recipes, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = new SaltConnector(gatewayConfig, restDebug)) {
            PillarSave scriptPillarSave = new PillarSave(sc, recipes);
            Callable<Boolean> saltPillarRunner = runner(scriptPillarSave, exitCriteria, exitCriteriaModel);
            Future<Boolean> saltPillarRunnerFuture = getParallelOrchestratorComponentRunner().submit(saltPillarRunner);
            saltPillarRunnerFuture.get();

            for (List<RecipeModel> recipeList : recipes.values()) {
                for (RecipeModel model : recipeList) {
                    if (model.getPreInstall() != null) {
                        uploadRecipe(sc, model.getName(), model.getPreInstall(), RecipeExecutionPhase.PRE);
                    }
                    if (model.getPostInstall() != null) {
                        uploadRecipe(sc, model.getName(), model.getPostInstall(), RecipeExecutionPhase.POST);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred during recipe upload", e);
            throw new CloudbreakOrchestratorFailedException(e);
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
    public int getMaxBootstrapNodes() {
        return MAX_NODES;
    }

    @Override
    public Map<String, String> getMembers(GatewayConfig gatewayConfig, List<String> privateIps) throws CloudbreakOrchestratorException {
        try (SaltConnector saltConnector = new SaltConnector(gatewayConfig, restDebug)) {
            return saltConnector.members(privateIps);
        } catch (IOException e) {
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    private void runNewService(SaltConnector sc, BaseSaltJobRunner baseSaltJobRunner, ExitCriteriaModel exitCriteriaModel) throws ExecutionException,
            InterruptedException {
        SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(sc, baseSaltJobRunner);
        Callable<Boolean> saltJobRunBootstrapRunner = runner(saltJobIdTracker, exitCriteria, exitCriteriaModel);
        Future<Boolean> saltJobRunBootstrapFuture = getParallelOrchestratorComponentRunner().submit(saltJobRunBootstrapRunner);
        saltJobRunBootstrapFuture.get();
    }

    private void runSaltCommand(SaltConnector sc, BaseSaltJobRunner baseSaltJobRunner, ExitCriteriaModel exitCriteriaModel) throws ExecutionException,
            InterruptedException {
        SaltCommandTracker saltCommandTracker = new SaltCommandTracker(sc, baseSaltJobRunner);
        Callable<Boolean> saltCommandRunBootstrapRunner = runner(saltCommandTracker, exitCriteria, exitCriteriaModel);
        Future<Boolean> saltCommandRunBootstrapFuture = getParallelOrchestratorComponentRunner().submit(saltCommandRunBootstrapRunner);
        saltCommandRunBootstrapFuture.get();
    }

    private void executeRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel, RecipeExecutionPhase phase)
            throws CloudbreakOrchestratorFailedException {
        try (SaltConnector sc = new SaltConnector(gatewayConfig, restDebug)) {
            // add 'recipe' grain to all nodes
            Set<String> targets = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new GrainAddRunner(targets, allNodes, "recipes", phase.value(), Compound.CompoundType.IP), exitCriteriaModel);

            Set<String> all = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new SyncGrainsRunner(all, allNodes), exitCriteriaModel);
            runNewService(sc, new HighStateRunner(all, allNodes), exitCriteriaModel);

            // remove 'recipe' grain from all nodes
            targets = allNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
            runSaltCommand(sc, new GrainRemoveRunner(targets, allNodes, "recipes", phase.value(), Compound.CompoundType.IP), exitCriteriaModel);
        } catch (Exception e) {
            LOGGER.error("Error occurred during recipe execution", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    private ParallelOrchestratorComponentRunner getParallelOrchestratorComponentRunner() {
        return parallelOrchestratorComponentRunner;
    }

    private Callable<Boolean> runner(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel) {
        return new OrchestratorBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap(), MAX_RETRY_COUNT, SLEEP_TIME);
    }

    private void uploadSaltConfig(SaltConnector saltConnector) throws IOException {
        byte[] byteArray = zipSaltConfig();
        LOGGER.info("Upload salt.zip to server");
        saltConnector.upload("/srv", "salt.zip", new ByteArrayInputStream(byteArray));
    }

    private byte[] zipSaltConfig() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ZipOutputStream zout = new ZipOutputStream(baos);
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Map<String, List<Resource>> structure = new TreeMap<>();
            for (Resource resource : resolver.getResources("classpath*:salt/**")) {
                String path = resource.getURL().getPath();
                String dir = path.substring(path.indexOf("/salt") + "/salt".length(), path.lastIndexOf("/") + 1);
                List<Resource> list = structure.get(dir);
                if (list == null) {
                    list = new ArrayList<>();
                }
                structure.put(dir, list);
                if (!path.endsWith("/")) {
                    list.add(resource);
                }
            }
            for (String dir : structure.keySet()) {
                zout.putNextEntry(new ZipEntry(dir));
                for (Resource resource : structure.get(dir)) {
                    LOGGER.info("Zip salt entry: {}", resource.getFilename());
                    zout.putNextEntry(new ZipEntry(dir + resource.getFilename()));
                    InputStream inputStream = resource.getInputStream();
                    byte[] bytes = IOUtils.toByteArray(inputStream);
                    zout.write(bytes);
                    zout.closeEntry();
                }
            }
            zout.close();
            baos.close();
        } catch (IOException e) {
            LOGGER.error("Failed to zip salt configurations", e);
            throw new IOException("Failed to zip salt configurations", e);
        }
        return baos.toByteArray();
    }

    private void uploadRecipe(SaltConnector sc, String name, String recipe, RecipeExecutionPhase phase) {
        final byte[] recipeBytes = recipe.getBytes(StandardCharsets.UTF_8);
        LOGGER.info("Upload '{}' recipe: {}", phase.value(), name);
        try {
            if (RecipeExecutionPhase.PRE.equals(phase)) {
                sc.upload("/srv/salt/pre-recipes/scripts", name, new ByteArrayInputStream(recipeBytes));
            } else {
                sc.upload("/srv/salt/post-recipes/scripts", name, new ByteArrayInputStream(recipeBytes));
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot upload recipe: {}", recipe);
        }
    }
}
