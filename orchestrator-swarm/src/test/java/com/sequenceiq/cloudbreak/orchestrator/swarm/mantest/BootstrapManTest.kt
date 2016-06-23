package com.sequenceiq.cloudbreak.orchestrator.swarm.mantest

import java.util.HashSet
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel
import com.sequenceiq.cloudbreak.orchestrator.swarm.SwarmContainerOrchestrator

class BootstrapManTest {


    private val nodes = HashSet<Node>()

    @Throws(Exception::class)
    fun build() {

        val o = SwarmContainerOrchestrator()
        o.init(SingleContainerRunner(), NoExit())

        val gatewayConfig = GatewayConfig("104.41.154.195", "10.0.0.8", 8443, "/Users/akanto/prj/cbd-test/certs/stack-4100")

        add("10.0.0.8", "104.41.154.195", "cbgateway-3-geza2-20150817160005")
        /*add("10.0.0.7", "23.96.84.101", "hostgroupclient1-2-geza2-20150817155818");
        add("10.0.0.9", "137.135.76.82", "hostgroupmaster1-4-geza2-20150817160217");
        add("10.0.0.10", "137.135.77.218", "hostgroupmaster2-5-geza2-20150817160343");
        add("10.0.0.6", "191.237.28.52", "hostgroupmaster3-1-geza2-20150817155642");
        add("10.0.0.5", "191.236.56.236", "hostgroupslave1-0-geza2-20150817155532");*/

        /*GatewayConfig gatewayConfig = new GatewayConfig("137.117.166.188", "10.0.0.6", "/Users/akanto/prj/cbd-test/certs/stack-4200");

        add("10.0.0.6", "137.117.166.188", "cbgateway-1-d2minimal-20150818120046");
        add("10.0.0.5", "137.117.167.157", "master-0-d2minimal-20150818115910");
        add("10.0.0.7", "137.117.161.31", "slave1-2-d2minimal-20150818120226");
        add("10.0.0.8", "23.97.219.211", "slave1-3-d2minimal-20150818120400");
        add("10.0.0.9", "168.63.108.25", "slave1-4-d2minimal-20150818120547");
        add("10.0.0.10", "65.52.139.9", "slave1-5-d2minimal-20150818120736");*/

        //o.bootstrap(gatewayConfig, nodes, CONSUL_SERVER_SIZE, "/hadoopfs/fs1", new NoExitModel());
        //if(true) return;

        /*ContainerOrchestratorCluster cluster = new ContainerOrchestratorCluster(gatewayConfig, nodes);
        o.startRegistrator(cluster, new ContainerConfig("sequenceiq/registrator", "v5.2"), new NoExitModel());

        for (int i = 0; i < LOOP_SIZE; i++) {
            LOGGER.info("Iteration: {}", i);
            o.startConsulWatches(cluster, new ContainerConfig("sequenceiq/docker-consul-watch-plugn", "2.0.0-consul"), new LogVolumePath("/hadoopfs/fs1",
                    "/hadoopfs/fs1"), new NoExitModel());
        }*/

        LOGGER.info("FINISHED")

    }

    private fun add(privateIp: String, publicIp: String, hostname: String) {
        val volumes = HashSet<String>()
        volumes.add("/hadoopfs/fs1")
        val n = Node(privateIp, publicIp, hostname, volumes)
        nodes.add(n)
    }

    private class NoExit : ExitCriteria {

        override fun isExitNeeded(exitCriteriaModel: ExitCriteriaModel): Boolean {
            return false
        }

        override fun exitMessage(): String {
            return "NO EXIT"
        }
    }

    private class NoExitModel : ExitCriteriaModel()


    private class SingleContainerRunner : ParallelOrchestratorComponentRunner {

        private val es = Executors.newFixedThreadPool(PARALLELISM)

        override fun submit(callable: Callable<Boolean>): Future<Boolean> {
            return es.submit(callable)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(BootstrapManTest::class.java)

        private val PARALLELISM = 1


        private val CONSUL_SERVER_SIZE = 1

        private val LOOP_SIZE = 10

        @Throws(Exception::class)
        @JvmStatic fun main(args: Array<String>) {
            val t = BootstrapManTest()
            t.build()
        }
    }
}
