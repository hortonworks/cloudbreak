package com.sequenceiq.cloudbreak.orchestrator.salt;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.AmbariRunBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.ConsulPillarBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.ConsulRunBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.ON_HOST;
import static java.util.Arrays.asList;

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
            ConsulPillarBootstrap pillarBootstrap = new ConsulPillarBootstrap(sc, consulServers);
            Callable<Boolean> saltPillarRunner = runner(pillarBootstrap, exitCriteria, exitCriteriaModel);
            Future<Boolean> saltPillarRunnerFuture = getParallelOrchestratorComponentRunner().submit(saltPillarRunner);
            saltPillarRunnerFuture.get();

            SaltBootstrap saltBootstrap = new SaltBootstrap(sc, gatewayConfig, allIPs, consulServers);
            Callable<Boolean> saltBootstrapRunner = runner(saltBootstrap, exitCriteria, exitCriteriaModel);
            Future<Boolean> saltBootstrapRunnerFuture = getParallelOrchestratorComponentRunner().submit(saltBootstrapRunner);
            saltBootstrapRunnerFuture.get();

            ConsulRunBootstrap consulRunBootstrap = new ConsulRunBootstrap(sc);
            Callable<Boolean> consulRunBootstrapRunner = runner(consulRunBootstrap, exitCriteria, exitCriteriaModel);
            Future<Boolean> consulRunBootstrapRunnerAppFuture = getParallelOrchestratorComponentRunner().submit(consulRunBootstrapRunner);
            consulRunBootstrapRunnerAppFuture.get();
        } catch (Exception e) {
            LOGGER.error("Error occurred under the consul bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void bootstrapNewNodes(GatewayConfig gatewayConfig, Set<Node> targets, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        Set<String> newAddresses = prepareTargets(gatewayConfig, targets);
        try (SaltConnector sc = new SaltConnector(gatewayConfig, restDebug)) {
            SaltBootstrap saltBootstrap = new SaltBootstrap(sc, gatewayConfig, newAddresses, Collections.EMPTY_SET);
            Callable<Boolean> saltBootstrapRunner = runner(saltBootstrap, exitCriteria, exitCriteriaModel);
            Future<Boolean> saltBootstrapRunnerFuture = getParallelOrchestratorComponentRunner().submit(saltBootstrapRunner);
            saltBootstrapRunnerFuture.get();

            ConsulRunBootstrap consulRunBootstrap = new ConsulRunBootstrap(sc);
            Callable<Boolean> consulRunBootstrapRunner = runner(consulRunBootstrap, exitCriteria, exitCriteriaModel);
            Future<Boolean> consulRunBootstrapRunnerAppFuture = getParallelOrchestratorComponentRunner().submit(consulRunBootstrapRunner);
            consulRunBootstrapRunnerAppFuture.get();
        } catch (Exception e) {
            LOGGER.error("Error occurred during salt upscale", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void runService(GatewayConfig gatewayConfig, Set<String> agents, OrchestrationCredential cred, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try (SaltConnector sc = new SaltConnector(gatewayConfig, restDebug)) {
            AmbariRunBootstrap ambariRunBootstrap = new AmbariRunBootstrap(sc);
            Callable<Boolean> ambariRunBootstrapRunner = runner(ambariRunBootstrap, exitCriteria, exitCriteriaModel);
            Future<Boolean> ambariRunBootstrapFuture = getParallelOrchestratorComponentRunner().submit(ambariRunBootstrapRunner);
            ambariRunBootstrapFuture.get();
        } catch (Exception e) {
            LOGGER.error("Error occurred during ambari bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public void tearDown(GatewayConfig gatewayConfig, List<String> ips) throws CloudbreakOrchestratorException {
        try (SaltConnector saltConnector = new SaltConnector(gatewayConfig, restDebug)) {
            SaltStates.removeMinions(saltConnector, ips);
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
        if (saltConnector.health().getStatusCode() == HttpStatus.OK.value()) {
            return true;
        }
        return false;
    }

    @Override
    public String name() {
        return ON_HOST;
    }

    @Override
    public int getMaxBootstrapNodes() {
        return MAX_NODES;
    }

    private Set<String> prepareTargets(GatewayConfig gatewayConfig, Set<Node> targets) {
        Set<String> targetList = new HashSet<>(asList(gatewayConfig.getPrivateAddress()));
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
