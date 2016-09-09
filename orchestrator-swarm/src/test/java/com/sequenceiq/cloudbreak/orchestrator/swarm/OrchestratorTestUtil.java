package com.sequenceiq.cloudbreak.orchestrator.swarm;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public class OrchestratorTestUtil {

    private static final int EXECUTOR_SIZE = 3;

    private OrchestratorTestUtil() {
    }

    public static GatewayConfig gatewayConfig() {
        return new GatewayConfig("11.0.0.0", "11.0.0.0", 8443, "/tmp/certs");
    }

    public static ContainerOrchestratorCluster containerOrchestratorCluster(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return new ContainerOrchestratorCluster(gatewayConfig, nodes);
    }

    public static Set<Node> generateNodes(int count) {
        Set<Node> nodes = new HashSet<>();
        for (int i = 0; i < count; i++) {
            nodes.add(node((long) i));
        }
        return nodes;
    }

    public static ExitCriteriaModel exitCriteriaModel() {
        class SimpleExitCriteriaModel extends ExitCriteriaModel {

        }

        return new SimpleExitCriteriaModel();
    }

    public static LogVolumePath generateLogVolume() {
        return new LogVolumePath("/hadoopfs/logs", "/var/log");
    }

    public static Callable<Boolean> createRunner(OrchestratorBootstrap orchestratorBootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel,
            Map<String, String> mdcReplica) {
        class TestContainerBootstrapRunner implements Callable<Boolean> {

            private final OrchestratorBootstrap containerBootstrap;
            private final Map<String, String> mdcMap;
            private final ExitCriteria exitCriteria;
            private final ExitCriteriaModel exitCriteriaModel;

            private TestContainerBootstrapRunner(OrchestratorBootstrap containerBootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel,
                    Map<String, String> mdcReplica) {
                this.containerBootstrap = containerBootstrap;
                this.mdcMap = mdcReplica;
                this.exitCriteria = exitCriteria;
                this.exitCriteriaModel = exitCriteriaModel;
            }

            @Override
            public Boolean call() throws Exception {
                return containerBootstrap.call();
            }
        }

        return new TestContainerBootstrapRunner(orchestratorBootstrap, exitCriteria, exitCriteriaModel, mdcReplica);
    }

    public static OrchestratorBootstrap containerBootstrap() {
        class TestOrchestratorBootstrap implements OrchestratorBootstrap {

            @Override
            public Boolean call() throws Exception {
                return containerBootstrap().call();
            }
        }

        return new TestOrchestratorBootstrap();
    }

    public static ParallelOrchestratorComponentRunner parallelContainerRunner() {
        class TestParallelOrchestratorComponentRunner implements ParallelOrchestratorComponentRunner {

            @Override
            public Future<Boolean> submit(Callable<Boolean> callable) {
                ExecutorService executorService = Executors.newFixedThreadPool(EXECUTOR_SIZE);
                return executorService.submit(callable);
            }
        }

        return new TestParallelOrchestratorComponentRunner();
    }

    public static ExitCriteria exitCriteria() {
        class TestExitCriteria implements ExitCriteria {

            @Override
            public boolean isExitNeeded(ExitCriteriaModel exitCriteriaModel) {
                return false;
            }

            @Override
            public String exitMessage() {
                return "exit.";
            }
        }

        return new TestExitCriteria();
    }

    public static Node node(Long id) {
        Set<String> strings = new HashSet<>();
        strings.add("df" + id);

        return new Node("10.0.0." + id, "11.0.0." + id, id.toString(), strings);
    }
}
