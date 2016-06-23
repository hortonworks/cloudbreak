package com.sequenceiq.cloudbreak.orchestrator.salt.poller

import java.util.Collections
import java.util.HashSet
import java.util.stream.Collectors

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates

class SaltBootstrap(private val sc: SaltConnector, private val gatewayConfig: GatewayConfig, private var targets: MutableSet<Node>?) : OrchestratorBootstrap {
    private val originalTargets: Set<Node>

    init {
        this.originalTargets = Collections.unmodifiableSet(targets)
    }

    @Throws(Exception::class)
    override fun call(): Boolean? {
        if (!targets!!.isEmpty()) {
            LOGGER.info("Missing targets for SaltBootstrap: {}", targets)

            val saltAction = createBootstrap()
            val responses = sc.action(saltAction)

            val failedTargets = HashSet<Node>()

            LOGGER.info("Salt run response: {}", responses)
            for (genericResponse in responses.responses!!) {
                if (genericResponse.statusCode != HttpStatus.OK.value()) {
                    LOGGER.info("Successfully distributed salt run to: " + genericResponse.address!!)
                    val address = genericResponse.address!!.split(":".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0]
                    failedTargets.addAll(originalTargets.stream().filter({ a -> a.privateIp == address }).collect(Collectors.toList<Node>()))
                }
            }
            targets = failedTargets

            if (!targets!!.isEmpty()) {
                LOGGER.info("Missing nodes to run salt: %s", targets)
                throw CloudbreakOrchestratorFailedException("There are missing nodes from salt: " + targets!!)
            }
        }

        val networkResult = SaltStates.networkInterfaceIP(sc, Glob.ALL).resultGroupByIP
        originalTargets.forEach { node ->
            if (!networkResult.containsKey(node.privateIp)) {
                LOGGER.info("Salt-minion is not responding on host: {}, yet", node)
                targets!!.add(node)
            }
        }
        if (!targets!!.isEmpty()) {
            throw CloudbreakOrchestratorFailedException("There are missing nodes from salt: " + targets!!)
        }
        return true
    }

    private fun createBootstrap(): SaltAction {
        val saltAction = SaltAction(SaltActionType.RUN)

        if (targets!!.stream().map(Function<Node, String> { it.getPrivateIp() }).collect(Collectors.toList<String>()).contains(gatewayPrivateIp)) {
            saltAction.server = gatewayPrivateIp
            val saltMaster = targets!!.stream().filter({ n -> n.privateIp == gatewayPrivateIp }).findFirst().get()
            saltAction.addMinion(createMinion(saltMaster))
        }
        for (minion in targets!!) {
            if (minion.privateIp != gatewayPrivateIp) {
                saltAction.addMinion(createMinion(minion))
            }
        }
        return saltAction
    }

    private fun createMinion(node: Node): Minion {
        val minion = Minion()
        minion.address = node.privateIp
        minion.roles = emptyList<String>()
        minion.server = gatewayPrivateIp
        minion.hostGroup = node.hostGroup
        return minion
    }

    private val gatewayPrivateIp: String
        get() = gatewayConfig.privateAddress

    override fun toString(): String {
        return "SaltBootstrap{"
        +"gatewayConfig=" + gatewayConfig
        +", originalTargets=" + originalTargets
        +", targets=" + targets
        +'}'
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(SaltBootstrap::class.java)
    }
}
