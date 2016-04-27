package com.sequenceiq.cloudbreak.orchestrator.onhost;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.ON_HOST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.SimpleHostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.onhost.client.OnHostClient;
import com.sequenceiq.cloudbreak.orchestrator.onhost.poller.AmbariRunBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.onhost.poller.ConsulConfigDistributeBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.onhost.poller.ConsulRunBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

@Component
public class OnHostOrchestrator extends SimpleHostOrchestrator {
    public static final String PORT = "7070";
    public static final int MAX_NODES = 5000;

    private static final Logger LOGGER = LoggerFactory.getLogger(OnHostOrchestrator.class);

    @Override
    public void validateApiEndpoint(OrchestrationCredential cred) throws CloudbreakOrchestratorException {
        return;
    }

    @Override
    public void runService(GatewayConfig gatewayConfig, Set<String> agents,
            OrchestrationCredential cred, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        OnHostClient onHostClient = new OnHostClient(gatewayConfig.getPublicAddress(), gatewayConfig.getPrivateAddress(), agents, port());

        try {
            AmbariRunBootstrap ambariRunBootstrap = new AmbariRunBootstrap(onHostClient);
            Callable<Boolean> ambariRunBootstrapRunner = runner(ambariRunBootstrap, getExitCriteria(), exitCriteriaModel);
            Future<Boolean> ambariRunBootstrapRunnerAppFuture = getParallelOrchestratorComponentRunner().submit(ambariRunBootstrapRunner);
            ambariRunBootstrapRunnerAppFuture.get();
        } catch (Exception e) {
            LOGGER.error("Error occured under the ambari bootstrap", e);
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
    public String name() {
        return ON_HOST;
    }

    @Override
    public String port() {
        return PORT;
    }

    @Override
    public void bootstrap(GatewayConfig gatewayConfig, Set<Node> targets, int consulServerCount, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        OnHostClient onHostClient = new OnHostClient(gatewayConfig.getPublicAddress(), gatewayConfig.getPrivateAddress(),
                prepareTargets(gatewayConfig, targets), port());

        try {
            ConsulConfigDistributeBootstrap consulConfigDistributeBootstrap = new ConsulConfigDistributeBootstrap(onHostClient);
            Callable<Boolean> consulConfigDistributeBootstrapRunner = runner(consulConfigDistributeBootstrap, getExitCriteria(), exitCriteriaModel);
            Future<Boolean> consulConfigDistributeBootstrapAppFuture = getParallelOrchestratorComponentRunner().submit(consulConfigDistributeBootstrapRunner);
            consulConfigDistributeBootstrapAppFuture.get();


            ConsulRunBootstrap consulRunBootstrap = new ConsulRunBootstrap(onHostClient);
            Callable<Boolean> consulRunBootstrapRunner = runner(consulRunBootstrap, getExitCriteria(), exitCriteriaModel);
            Future<Boolean> consulRunBootstrapRunnerAppFuture = getParallelOrchestratorComponentRunner().submit(consulRunBootstrapRunner);
            consulRunBootstrapRunnerAppFuture.get();

        } catch (Exception e) {
            LOGGER.error("Error occurred under the consul bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    private Set<String> prepareTargets(GatewayConfig gatewayConfig, Set<Node> targets) {
        Set<String> targetList = new HashSet<>(Arrays.asList(gatewayConfig.getPrivateAddress()));
        for (Node node : targets) {
            targetList.add(node.getPrivateIp());
        }
        return targetList;
    }

    @Override
    public void bootstrapNewNodes(GatewayConfig gatewayConfig, Set<Node> nodes, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {

    }

    @Override
    public boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig) {
        OnHostClient onHostClient = new OnHostClient(gatewayConfig.getPublicAddress(), gatewayConfig.getPrivateAddress(), port());
        return onHostClient.info();
    }

    @Override
    public int getMaxBootstrapNodes() {
        return MAX_NODES;
    }

    private Callable<Boolean> runner(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel) {
        return new OrchestratorBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap());
    }
}
