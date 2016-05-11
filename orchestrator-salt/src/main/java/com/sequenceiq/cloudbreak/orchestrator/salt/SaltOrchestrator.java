package com.sequenceiq.cloudbreak.orchestrator.salt;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SALT;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.AmbariAgentAddRoleChecker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.AmbariServerAddRoleChecker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.ConsulChecker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.HighStateChecker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.KerberosAddRoleChecker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.PillarSave;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltCommandTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobIdTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.SyncGrainsChecker;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

@Component
public class SaltOrchestrator implements HostOrchestrator {

    private static final int MAX_NODES = 5000;

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltOrchestrator.class);

    @Value("${rest.debug:false}")
    private boolean restDebug;

    private ParallelOrchestratorComponentRunner parallelOrchestratorComponentRunner;
    private ExitCriteria exitCriteria;

    @Override
    public void init(ParallelOrchestratorComponentRunner parallelOrchestratorComponentRunner, ExitCriteria exitCriteria) {
        this.parallelOrchestratorComponentRunner = parallelOrchestratorComponentRunner;
        this.exitCriteria = exitCriteria;
    }

    @Override
    public void bootstrap(GatewayConfig gatewayConfig, Set<Node> targets, int consulServerCount, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        Set<String> allIPs = prepareTargets(gatewayConfig, targets);

        Set<String> consulServers = new HashSet<>(asList(gatewayConfig.getPrivateAddress()));
        Iterator<String> iterator = allIPs.iterator();
        while (consulServers.size() < consulServerCount) {
            consulServers.add(iterator.next());
        }

        try (SaltConnector sc = new SaltConnector(gatewayConfig, restDebug)) {
            PillarSave consulPillarSave = new PillarSave(sc, consulServers);
            Callable<Boolean> saltPillarRunner = runner(consulPillarSave, exitCriteria, exitCriteriaModel);
            Future<Boolean> saltPillarRunnerFuture = getParallelOrchestratorComponentRunner().submit(saltPillarRunner);
            saltPillarRunnerFuture.get();

            SaltBootstrap saltBootstrap = new SaltBootstrap(sc, gatewayConfig, allIPs, consulServers);
            Callable<Boolean> saltBootstrapRunner = runner(saltBootstrap, exitCriteria, exitCriteriaModel);
            Future<Boolean> saltBootstrapRunnerFuture = getParallelOrchestratorComponentRunner().submit(saltBootstrapRunner);
            saltBootstrapRunnerFuture.get();

            runNewService(sc, new ConsulChecker(Glob.ALL), exitCriteriaModel);
        } catch (Exception e) {
            LOGGER.error("Error occurred under the consul bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void bootstrapNewNodes(GatewayConfig gatewayConfig, Set<Node> targets, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        Set<String> newAddresses = prepareTargets(null, targets);
        try (SaltConnector sc = new SaltConnector(gatewayConfig, restDebug)) {
            SaltBootstrap saltBootstrap = new SaltBootstrap(sc, gatewayConfig, newAddresses, Collections.EMPTY_SET);
            Callable<Boolean> saltBootstrapRunner = runner(saltBootstrap, exitCriteria, exitCriteriaModel);
            Future<Boolean> saltBootstrapRunnerFuture = getParallelOrchestratorComponentRunner().submit(saltBootstrapRunner);
            saltBootstrapRunnerFuture.get();

            runNewService(sc, new ConsulChecker(Glob.ALL), exitCriteriaModel);
        } catch (Exception e) {
            LOGGER.error("Error occurred during salt upscale", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void runService(GatewayConfig gatewayConfig, Set<String> nodeIPs, SaltPillarConfig pillarConfig, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try (SaltConnector sc = new SaltConnector(gatewayConfig, restDebug)) {
            for (Map.Entry<String, SaltPillarProperties> propertiesEntry : pillarConfig.getServicePillarConfig().entrySet()) {
                PillarSave pillarSave = new PillarSave(sc, propertiesEntry.getValue());
                Callable<Boolean> saltPillarRunner = runner(pillarSave, exitCriteria, exitCriteriaModel);
                Future<Boolean> saltPillarRunnerFuture = getParallelOrchestratorComponentRunner().submit(saltPillarRunner);
                saltPillarRunnerFuture.get();
            }

            Compound gwTarget = new Compound(gatewayConfig.getPrivateAddress());
            Compound agentNodes = new Compound(nodeIPs);

            // ambari server
            runSaltCommand(sc, new AmbariServerAddRoleChecker(gwTarget), exitCriteriaModel);
            // ambari agent
            runSaltCommand(sc, new AmbariAgentAddRoleChecker(agentNodes), exitCriteriaModel);
            // kerberos
            if (pillarConfig.getServicePillarConfig().containsKey("kerberos")) {
                runSaltCommand(sc, new KerberosAddRoleChecker(gwTarget), exitCriteriaModel);
            }
            runSaltCommand(sc, new SyncGrainsChecker(Glob.ALL), exitCriteriaModel);
            runNewService(sc, new HighStateChecker(Glob.ALL), exitCriteriaModel);
        } catch (Exception e) {
            LOGGER.error("Error occurred during ambari bootstrap", e);
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
    public List<String> getMissingNodes(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return new ArrayList<>();
    }

    @Override
    public boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig) {
        SaltConnector saltConnector = new SaltConnector(gatewayConfig, restDebug);
        try {
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

    private Set<String> prepareTargets(GatewayConfig gatewayConfig, Set<Node> targets) {
        Set<String> targetList = new HashSet<>();
        if (gatewayConfig != null) {
            targetList.add(gatewayConfig.getPrivateAddress());
        }
        for (Node node : targets) {
            targetList.add(node.getPrivateIp());
        }
        return targetList;
    }

    private ParallelOrchestratorComponentRunner getParallelOrchestratorComponentRunner() {
        return parallelOrchestratorComponentRunner;
    }

    private Callable<Boolean> runner(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel) {
        return new OrchestratorBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap());
    }
}
