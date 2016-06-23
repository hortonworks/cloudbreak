package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedExitCriteriaModel

import java.util.ArrayList
import java.util.HashSet
import java.util.stream.Collectors
import java.util.stream.Stream

import javax.inject.Inject

import org.apache.commons.codec.binary.Base64
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Recipe
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel
import com.sequenceiq.cloudbreak.service.TlsSecurityService

@Component
class OrchestratorRecipeExecutor {

    @Inject
    private val hostOrchestratorResolver: HostOrchestratorResolver? = null
    @Inject
    private val tlsSecurityService: TlsSecurityService? = null

    @Throws(CloudbreakException::class)
    fun uploadRecipes(stack: Stack, hostGroups: Set<HostGroup>) {
        val hostOrchestrator = hostOrchestratorResolver!!.get(stack.orchestrator.type)
        val recipeMap = hostGroups.stream().filter({ hg -> !hg.getRecipes().isEmpty() }).collect(Collectors.toMap<HostGroup, String, List<RecipeModel>>(Function<HostGroup, String> { it.getName() }) { h -> convert(h.recipes) })
        val gatewayInstance = stack.gatewayInstanceGroup.instanceMetaData.iterator().next()
        val gatewayConfig = tlsSecurityService!!.buildGatewayConfig(stack.id,
                gatewayInstance.publicIpWrapper, stack.gatewayPort, gatewayInstance.privateIp, gatewayInstance.discoveryFQDN)
        try {
            hostOrchestrator.uploadRecipes(gatewayConfig, recipeMap, collectNodes(stack),
                    clusterDeletionBasedExitCriteriaModel(stack.id, stack.cluster.id))
        } catch (e: CloudbreakOrchestratorFailedException) {
            throw CloudbreakException(e)
        }

    }

    @Throws(CloudbreakException::class)
    fun preInstall(stack: Stack) {
        val hostOrchestrator = hostOrchestratorResolver!!.get(stack.orchestrator.type)
        val gatewayInstance = stack.gatewayInstanceGroup.instanceMetaData.iterator().next()
        val gatewayConfig = tlsSecurityService!!.buildGatewayConfig(stack.id,
                gatewayInstance.publicIpWrapper, stack.gatewayPort, gatewayInstance.privateIp, gatewayInstance.discoveryFQDN)
        try {
            hostOrchestrator.preInstallRecipes(gatewayConfig, collectNodes(stack),
                    clusterDeletionBasedExitCriteriaModel(stack.id, stack.cluster.id))
        } catch (e: CloudbreakOrchestratorFailedException) {
            throw CloudbreakException(e)
        }

    }

    @Throws(CloudbreakException::class)
    fun postInstall(stack: Stack) {
        val hostOrchestrator = hostOrchestratorResolver!!.get(stack.orchestrator.type)
        val gatewayInstance = stack.gatewayInstanceGroup.instanceMetaData.iterator().next()
        val gatewayConfig = tlsSecurityService!!.buildGatewayConfig(stack.id,
                gatewayInstance.publicIpWrapper, stack.gatewayPort, gatewayInstance.privateIp, gatewayInstance.discoveryFQDN)
        try {
            hostOrchestrator.postInstallRecipes(gatewayConfig, collectNodes(stack),
                    clusterDeletionBasedExitCriteriaModel(stack.id, stack.cluster.id))
        } catch (e: CloudbreakOrchestratorFailedException) {
            throw CloudbreakException(e)
        }

    }

    private fun convert(recipes: Set<Recipe>): List<RecipeModel> {
        val result = ArrayList<RecipeModel>()
        for (recipe in recipes) {
            recipe.plugins.keys.stream().filter({ rawRecipe -> rawRecipe.startsWith("base64://") }).forEach({ rawRecipe ->
                val decodedRecipe = String(Base64.decodeBase64(rawRecipe.replaceFirst("base64://".toRegex(), "")))
                val recipeModel = RecipeModel(recipe.name)
                val recipeMap = Stream.of<String>(*decodedRecipe.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()).collect<Map<String, String>, Any>(Collectors.toMap<String, String, String>({ s -> s.substring(0, s.indexOf(":")) }) { s -> s.substring(s.indexOf(":") + 1) })
                if (recipeMap.containsKey(PRE_INSTALL_TAG)) {
                    recipeModel.preInstall = String(Base64.decodeBase64(recipeMap[PRE_INSTALL_TAG]))
                }
                if (recipeMap.containsKey(POST_INSTALL_TAG)) {
                    recipeModel.postInstall = String(Base64.decodeBase64(recipeMap[POST_INSTALL_TAG]))
                }
                recipeModel.keyValues = recipe.keyValues
                result.add(recipeModel)
            })
        }
        return result
    }

    private fun collectNodes(stack: Stack): Set<Node> {
        val agents = HashSet<Node>()
        for (instanceGroup in stack.instanceGroups) {
            for (instanceMetaData in instanceGroup.instanceMetaData) {
                val node = Node(instanceMetaData.privateIp, instanceMetaData.publicIp, instanceMetaData.discoveryFQDN)
                node.hostGroup = instanceGroup.groupName
                agents.add(node)
            }
        }
        return agents
    }

    companion object {

        private val PRE_INSTALL_TAG = "recipe-pre-install"
        private val POST_INSTALL_TAG = "recipe-post-install"
    }
}
