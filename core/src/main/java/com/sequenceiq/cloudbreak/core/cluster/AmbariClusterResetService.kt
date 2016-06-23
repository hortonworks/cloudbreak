package com.sequenceiq.cloudbreak.core.cluster

import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedExitCriteriaModel

import java.util.Collections

import javax.inject.Inject

import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.util.StackUtil

@Service
class AmbariClusterResetService {

    @Inject
    private val tlsSecurityService: TlsSecurityService? = null

    @Inject
    private val orchestratorTypeResolver: OrchestratorTypeResolver? = null

    @Inject
    private val hostOrchestratorResolver: HostOrchestratorResolver? = null

    @Inject
    private val stackRepository: StackRepository? = null

    @Throws(CloudbreakOrchestratorException::class)
    fun resetCluster(stackId: Long?) {
        val stack = stackRepository!!.findOneWithLists(stackId)
        val gatewayInstanceGroup = stack.gatewayInstanceGroup
        try {
            val gatewayInstance = gatewayInstanceGroup.instanceMetaData.iterator().next()
            val gatewayConfig = tlsSecurityService!!.buildGatewayConfig(stack.id, gatewayInstance.publicIpWrapper,
                    stack.gatewayPort, gatewayInstance.privateIp, gatewayInstance.discoveryFQDN)
            val orchestratorType = orchestratorTypeResolver!!.resolveType(stack.orchestrator.type)
            if (orchestratorType.hostOrchestrator()) {
                val hostOrchestrator = hostOrchestratorResolver!!.get(stack.orchestrator.type)
                val gatewayFQDN = setOf<String>(gatewayInstance.discoveryFQDN)
                val exitCriteriaModel = clusterDeletionBasedExitCriteriaModel(stack.id, stack.cluster.id)
                hostOrchestrator.resetAmbari(gatewayConfig, gatewayFQDN, StackUtil.collectNodes(stack), exitCriteriaModel)
            } else {
                throw UnsupportedOperationException("ambari reset cluster works only with host orchestrator")
            }
        } catch (e: CloudbreakException) {
            throw CloudbreakOrchestratorFailedException(e)
        }

    }

}
