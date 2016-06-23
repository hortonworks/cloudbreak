package com.sequenceiq.cloudbreak.core.bootstrap.service.host

import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedExitCriteriaModel
import java.util.Collections.singletonMap

import java.util.HashMap
import java.util.HashSet

import javax.inject.Inject

import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarConfig
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties
import com.sequenceiq.cloudbreak.repository.HostGroupRepository
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.cluster.ClusterService

@Component
class ClusterHostServiceRunner {

    @Inject
    private val stackRepository: StackRepository? = null
    @Inject
    private val hostOrchestratorResolver: HostOrchestratorResolver? = null
    @Inject
    private val tlsSecurityService: TlsSecurityService? = null
    @Inject
    private val conversionService: ConversionService? = null
    @Inject
    private val clusterService: ClusterService? = null
    @Inject
    private val hostGroupRepository: HostGroupRepository? = null
    @Inject
    private val instanceMetaDataRepository: InstanceMetaDataRepository? = null

    @Throws(CloudbreakException::class)
    fun runAmbariServices(stack: Stack) {
        try {
            val gateway = stack.gatewayInstanceGroup
            val nodes = collectNodes(stack)
            val hostOrchestrator = hostOrchestratorResolver!!.get(stack.orchestrator.type)
            val gatewayInstance = gateway.instanceMetaData.iterator().next()
            val gatewayConfig = tlsSecurityService!!.buildGatewayConfig(stack.id,
                    gatewayInstance.publicIpWrapper, stack.gatewayPort, gatewayInstance.privateIp, gatewayInstance.discoveryFQDN)
            val cluster = stack.cluster
            val servicePillar = HashMap<String, SaltPillarProperties>()
            if (cluster.isSecure) {
                val krb = HashMap<String, Any>()
                val kerberosConf = HashMap<String, String>()
                kerberosConf.put("masterKey", cluster.kerberosMasterKey)
                kerberosConf.put("user", cluster.kerberosAdmin)
                kerberosConf.put("password", cluster.kerberosPassword)
                krb.put("kerberos", kerberosConf)
                servicePillar.put("kerberos", SaltPillarProperties("/kerberos/init.sls", krb))
            }
            servicePillar.put("discovery", SaltPillarProperties("/discovery/init.sls", singletonMap<String, Any>("platform", stack.cloudPlatform())))
            val saltPillarConfig = SaltPillarConfig(servicePillar)
            hostOrchestrator.runService(gatewayConfig, nodes, saltPillarConfig, clusterDeletionBasedExitCriteriaModel(stack.id, cluster.id))
        } catch (e: CloudbreakOrchestratorCancelledException) {
            throw CancellationException(e.message)
        } catch (e: CloudbreakOrchestratorException) {
            throw CloudbreakException(e)
        }

    }

    @Throws(CloudbreakException::class)
    fun addAmbariServices(stackId: Long?, hostGroupName: String, scalingAdjustment: Int?): Map<String, String> {
        val candidates: Map<String, String>
        try {
            val stack = stackRepository!!.findOneWithLists(stackId)
            val cluster = stack.cluster
            val gateway = stack.gatewayInstanceGroup
            candidates = collectUpscaleCandidates(cluster.id, hostGroupName, scalingAdjustment)
            val allNodes = collectNodes(stack)
            val hostOrchestrator = hostOrchestratorResolver!!.get(stack.orchestrator.type)
            val gatewayInstance = gateway.instanceMetaData.iterator().next()
            val gatewayConfig = tlsSecurityService!!.buildGatewayConfig(stack.id,
                    gatewayInstance.publicIpWrapper, stack.gatewayPort, gatewayInstance.privateIp, gatewayInstance.discoveryFQDN)
            hostOrchestrator.runService(gatewayConfig, allNodes, SaltPillarConfig(), clusterDeletionBasedExitCriteriaModel(stack.id, cluster.id))
        } catch (e: CloudbreakOrchestratorCancelledException) {
            throw CancellationException(e.message)
        } catch (e: CloudbreakOrchestratorException) {
            throw CloudbreakException(e)
        }

        return candidates
    }

    private fun collectUpscaleCandidates(clusterId: Long?, hostGroupName: String, adjustment: Int?): Map<String, String>? {
        val hostGroup = hostGroupRepository!!.findHostGroupInClusterByName(clusterId, hostGroupName)
        if (hostGroup.constraint.instanceGroup != null) {
            val instanceGroupId = hostGroup.constraint.instanceGroup.id
            val unusedHostsInInstanceGroup = instanceMetaDataRepository!!.findUnusedHostsInInstanceGroup(instanceGroupId)
            val hostNames = HashMap<String, String>()
            for (instanceMetaData in unusedHostsInInstanceGroup) {
                hostNames.put(instanceMetaData.discoveryFQDN, instanceMetaData.privateIp)
                if (hostNames.size >= adjustment) {
                    break
                }
            }
            return hostNames
        }
        return null
    }

    @Throws(CloudbreakException::class, CloudbreakOrchestratorException::class)
    private fun collectNodes(stack: Stack): Set<Node> {
        val agents = HashSet<Node>()
        for (instanceGroup in stack.instanceGroups) {
            for (instanceMetaData in instanceGroup.instanceMetaData) {
                agents.add(Node(instanceMetaData.privateIp, instanceMetaData.publicIp, instanceMetaData.discoveryFQDN))
            }
        }
        return agents
    }

}
