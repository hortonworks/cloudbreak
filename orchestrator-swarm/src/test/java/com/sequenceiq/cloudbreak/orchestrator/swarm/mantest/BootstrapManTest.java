package com.sequenceiq.cloudbreak.orchestrator.swarm.mantest;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelContainerRunner;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.orchestrator.swarm.SwarmContainerOrchestrator;

public class BootstrapManTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapManTest.class);

    private static final int PARALLELISM = 5;


    private static final int CONSUL_SERVER_SIZE = 3;

    private static final int LOOP_SIZE = 50;


    private Set<Node> nodes = new HashSet<>();

    public static void main(String[] args) throws Exception {
        BootstrapManTest t = new BootstrapManTest();
        t.build();
    }

    public void build() throws Exception {

        SwarmContainerOrchestrator o = new SwarmContainerOrchestrator();
        o.init(new SingleContainerRunner(), new NoExit());
        GatewayConfig gatewayConfig = new GatewayConfig("23.97.150.238", "10.0.0.6", "/Users/akanto/prj/cbd-test/certs");

        add("10.0.0.6", "23.97.150.238", "cbgateway-1-swarmtest-20150812085930");
        add("10.0.0.5", "104.40.137.28", "master-0-swarmtest-20150812085817");
        add("10.0.0.7", "104.40.204.184", "slave1-2-swarmtest-20150812090155");
        add("10.0.0.8", "104.40.203.147", "slave1-3-swarmtest-20150812090346");
        add("10.0.0.9", "104.40.203.238", "slave1-4-swarmtest-20150812090521");

        o.bootstrap(gatewayConfig, nodes, CONSUL_SERVER_SIZE, "/hadoopfs/fs1", new NoExitModel());


        ContainerOrchestratorCluster cluster = new ContainerOrchestratorCluster(gatewayConfig, nodes);
        o.startRegistrator(cluster, "sequenceiq/registrator:v5.2", new NoExitModel());

        for (int i = 0; i < LOOP_SIZE; i++) {
            LOGGER.info("Iteration: {}", i);
            o.startConsulWatches(cluster, "sequenceiq/docker-consul-watch-plugn:2.0.0-consul", cluster.getNodes().size(),
                    new LogVolumePath("/hadoopfs/fs1", "/hadoopfs/fs1"), new NoExitModel());
        }

        LOGGER.info("FINISHED");

    }

    private void add(String privateIp, String publicIp, String hostname) {
        Set<String> volumes = new HashSet<>();
        volumes.add("/hadoopfs/fs1");
        Node n = new Node(privateIp, publicIp, hostname, volumes);
        nodes.add(n);
    }

    private static class NoExit implements ExitCriteria {

        @Override
        public boolean isExitNeeded(ExitCriteriaModel exitCriteriaModel) {
            return false;
        }

        @Override
        public String exitMessage() {
            return "NO EXIT";
        }
    }

    private static class NoExitModel extends ExitCriteriaModel {

    }


    private static class SingleContainerRunner implements ParallelContainerRunner {

        private ExecutorService es = Executors.newFixedThreadPool(PARALLELISM);

        @Override
        public Future<Boolean> submit(Callable<Boolean> callable) {
            return es.submit(callable);
        }

    }
}
