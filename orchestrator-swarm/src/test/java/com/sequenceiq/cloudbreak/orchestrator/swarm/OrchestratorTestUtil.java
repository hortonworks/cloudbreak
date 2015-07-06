package com.sequenceiq.cloudbreak.orchestrator.swarm;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orchestrator.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.orchestrator.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.orchestrator.ParallelContainerRunner;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;

public class OrchestratorTestUtil {

    private static final int EXECUTOR_SIZE = 3;

    private OrchestratorTestUtil() {
    }

    public static GatewayConfig gatewayConfig() {
        return new GatewayConfig("11.0.0.0", "/tmp/certs");
    }

    public static ContainerOrchestratorCluster containerOrchestratorCluster(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return new ContainerOrchestratorCluster(gatewayConfig, nodes);
    }

    public static Set<Node> generateNodes(int count) {
        Set<Node> nodes = new HashSet<>();
        for (int i = 0; i < count; i++) {
            nodes.add(node(Long.valueOf(i)));
        }
        return nodes;
    }

    public static ExitCriteriaModel exitCriteriaModel() {
        class SimpleExitCriteriaModel extends ExitCriteriaModel {

        }
        return new SimpleExitCriteriaModel();
    }

    public static Callable<Boolean> createRunner(ContainerBootstrap containerBootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel,
            Map<String, String> mdcReplica) {
        class TestContainerBootstrapRunner implements Callable<Boolean> {

            private final ContainerBootstrap containerBootstrap;
            private final Map<String, String> mdcMap;
            private final ExitCriteria exitCriteria;
            private final ExitCriteriaModel exitCriteriaModel;

            private TestContainerBootstrapRunner(ContainerBootstrap containerBootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel,
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
        return new TestContainerBootstrapRunner(containerBootstrap, exitCriteria, exitCriteriaModel, mdcReplica);
    }

    public static ContainerBootstrap containerBootstrap() {
        class TestContainerBootstrap implements ContainerBootstrap {

            @Override
            public Boolean call() throws Exception {
                return containerBootstrap().call();
            }
        }
        return new TestContainerBootstrap();
    }

    public static ParallelContainerRunner parallelContainerRunner() {
        class TestParallelContainerRunner implements ParallelContainerRunner {

            @Override
            public Future<Boolean> submit(Callable<Boolean> callable) {
                ExecutorService executorService = Executors.newFixedThreadPool(EXECUTOR_SIZE);
                return executorService.submit(callable);
            }
        }
        return new TestParallelContainerRunner();
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
