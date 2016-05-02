package com.sequenceiq.cloudbreak.orchestrator.onhost;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.onhost.client.OnHostClient;
import com.sequenceiq.cloudbreak.orchestrator.onhost.poller.AmbariRunBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.onhost.poller.ConsulRunBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.onhost.poller.ConsulRunUpscale;
import com.sequenceiq.cloudbreak.orchestrator.onhost.poller.SaltBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.ON_HOST;

@Component
public class OnHostOrchestrator implements HostOrchestrator {

    public static final String PORT = "443";
    public static final int MAX_NODES = 5000;

    private static final Logger LOGGER = LoggerFactory.getLogger(OnHostOrchestrator.class);


    private ParallelOrchestratorComponentRunner parallelOrchestratorComponentRunner;
    private ExitCriteria exitCriteria;

    @Override
    public void init(ParallelOrchestratorComponentRunner parallelOrchestratorComponentRunner, ExitCriteria exitCriteria) {
        this.parallelOrchestratorComponentRunner = parallelOrchestratorComponentRunner;
        this.exitCriteria = exitCriteria;
    }

    public ParallelOrchestratorComponentRunner getParallelOrchestratorComponentRunner() {
        return parallelOrchestratorComponentRunner;
    }

    protected ExitCriteria getExitCriteria() {
        return exitCriteria;
    }


    @Override
    public void runService(GatewayConfig gatewayConfig, Set<String> agents,
                           OrchestrationCredential cred, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {

        try {
            AmbariRunBootstrap ambariRunBootstrap = new AmbariRunBootstrap(gatewayConfig.getPublicAddress());

            Callable<Boolean> ambariRunBootstrapRunner = runner(ambariRunBootstrap, getExitCriteria(), exitCriteriaModel);
            Future<Boolean> ambariRunBootstrapFuture = getParallelOrchestratorComponentRunner().submit(ambariRunBootstrapRunner);
            ambariRunBootstrapFuture.get();

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
        OnHostClient onHostClient = new OnHostClient(gatewayConfig, prepareTargets(gatewayConfig, targets), port());

        try {
            SaltBootstrap saltBootstrap = new SaltBootstrap(onHostClient);
            Callable<Boolean> saltBootstrapRunner = runner(saltBootstrap, getExitCriteria(), exitCriteriaModel);
            Future<Boolean> saltBootstrapRunnerFuture = getParallelOrchestratorComponentRunner().submit(saltBootstrapRunner);
            saltBootstrapRunnerFuture.get();

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
    public void bootstrapNewNodes(GatewayConfig gatewayConfig, Set<Node> targets, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        Set<String> strings = prepareTargets(gatewayConfig, targets);
        OnHostClient onHostClient = new OnHostClient(gatewayConfig, strings, port());
        try {


            ConsulRunUpscale consulRunUpscale = new ConsulRunUpscale(onHostClient, strings);
            Callable<Boolean> consulRunUpscaleRunner = runner(consulRunUpscale, getExitCriteria(), exitCriteriaModel);
            Future<Boolean> consulRunUpscaleRunnerAppFuture = getParallelOrchestratorComponentRunner().submit(consulRunUpscaleRunner);
            consulRunUpscaleRunnerAppFuture.get();

        } catch (Exception e) {
            LOGGER.error("Error occurred under the consul bootstrap", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    @Override
    public boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig) {
        OnHostClient onHostClient = new OnHostClient(gatewayConfig, port());
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
