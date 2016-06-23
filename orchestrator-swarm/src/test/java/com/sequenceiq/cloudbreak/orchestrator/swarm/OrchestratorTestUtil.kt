package com.sequenceiq.cloudbreak.orchestrator.swarm

import java.util.HashSet
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestratorCluster
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner

object OrchestratorTestUtil {

    private val EXECUTOR_SIZE = 3

    fun gatewayConfig(): GatewayConfig {
        return GatewayConfig("11.0.0.0", "11.0.0.0", 8443, "/tmp/certs")
    }

    fun containerOrchestratorCluster(gatewayConfig: GatewayConfig, nodes: Set<Node>): ContainerOrchestratorCluster {
        return ContainerOrchestratorCluster(gatewayConfig, nodes)
    }

    fun generateNodes(count: Int): Set<Node> {
        val nodes = HashSet<Node>()
        for (i in 0..count - 1) {
            nodes.add(node(java.lang.Long.valueOf(i.toLong())))
        }
        return nodes
    }

    fun exitCriteriaModel(): ExitCriteriaModel {
        class SimpleExitCriteriaModel : ExitCriteriaModel()
        return SimpleExitCriteriaModel()
    }

    fun generateLogVolume(): LogVolumePath {
        return LogVolumePath("/hadoopfs/logs", "/var/log")
    }

    fun createRunner(orchestratorBootstrap: OrchestratorBootstrap, exitCriteria: ExitCriteria, exitCriteriaModel: ExitCriteriaModel,
                     mdcReplica: Map<String, String>): Callable<Boolean> {
        class TestContainerBootstrapRunner private constructor(private val containerBootstrap: OrchestratorBootstrap, private val exitCriteria: ExitCriteria, private val exitCriteriaModel: ExitCriteriaModel,
                                                               private val mdcMap: Map<String, String>) : Callable<Boolean> {

            @Throws(Exception::class)
            override fun call(): Boolean? {
                return containerBootstrap.call()
            }
        }
        return TestContainerBootstrapRunner(orchestratorBootstrap, exitCriteria, exitCriteriaModel, mdcReplica)
    }

    fun containerBootstrap(): OrchestratorBootstrap {
        class TestOrchestratorBootstrap : OrchestratorBootstrap {

            @Throws(Exception::class)
            override fun call(): Boolean? {
                return containerBootstrap().call()
            }
        }
        return TestOrchestratorBootstrap()
    }

    fun parallelContainerRunner(): ParallelOrchestratorComponentRunner {
        class TestParallelOrchestratorComponentRunner : ParallelOrchestratorComponentRunner {

            override fun submit(callable: Callable<Boolean>): Future<Boolean> {
                val executorService = Executors.newFixedThreadPool(EXECUTOR_SIZE)
                return executorService.submit(callable)
            }
        }
        return TestParallelOrchestratorComponentRunner()
    }

    fun exitCriteria(): ExitCriteria {
        class TestExitCriteria : ExitCriteria {

            override fun isExitNeeded(exitCriteriaModel: ExitCriteriaModel): Boolean {
                return false
            }

            override fun exitMessage(): String {
                return "exit."
            }
        }
        return TestExitCriteria()
    }

    fun node(id: Long?): Node {
        val strings = HashSet<String>()
        strings.add("df" + id!!)
        return Node("10.0.0." + id, "11.0.0." + id, id.toString(), strings)
    }
}
