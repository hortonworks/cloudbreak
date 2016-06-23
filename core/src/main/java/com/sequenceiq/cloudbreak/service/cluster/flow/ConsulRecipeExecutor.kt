package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_AGENT
import com.sequenceiq.cloudbreak.service.cluster.flow.RecipeEngine.DEFAULT_RECIPE_TIMEOUT

import java.util.Collections
import java.util.HashMap
import java.util.stream.Collectors
import javax.inject.Inject

import org.apache.commons.codec.binary.Base64
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.google.common.base.Function
import com.google.common.collect.Collections2
import com.google.common.collect.Iterables
import com.google.common.collect.Sets
import com.sequenceiq.cloudbreak.api.model.ExecutionType
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.CloudbreakRecipeSetupException
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Recipe
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig

@Component
class ConsulRecipeExecutor {

    @Inject
    private val instanceMetadataRepository: InstanceMetaDataRepository? = null
    @Inject
    private val pluginManager: PluginManager? = null
    @Inject
    private val tlsSecurityService: TlsSecurityService? = null

    @Throws(CloudbreakSecuritySetupException::class, CloudbreakRecipeSetupException::class)
    fun preInstall(stack: Stack, hostGroups: Set<HostGroup>) {
        setupRecipes(stack, hostGroups)
        executePreInstall(stack)
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun executePreInstall(stack: Stack, hostMetadata: Set<HostMetadata>) {
        pluginManager!!.triggerAndWaitForPlugins(stack, ConsulPluginEvent.PRE_INSTALL, DEFAULT_RECIPE_TIMEOUT, AMBARI_AGENT,
                emptyList<String>(), getHostnames(hostMetadata))
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun setupRecipesOnHosts(stack: Stack, recipes: Set<Recipe>, hostMetadata: Set<HostMetadata>) {
        uploadConsulRecipes(stack, recipes)
        val instances = instanceMetadataRepository!!.findNotTerminatedForStack(stack.id)
        installPluginsOnHosts(stack, recipes, hostMetadata, instances, true)
    }

    @Throws(CloudbreakException::class)
    fun configureSssd(stack: Stack, hostMetadata: Set<HostMetadata>?, sssdPayload: List<String>) {
        val hosts = if (hostMetadata == null) null else Sets.newHashSet(getHostnames(hostMetadata))
        pluginManager!!.triggerAndWaitForPlugins(stack, ConsulPluginEvent.SSSD_SETUP, DEFAULT_RECIPE_TIMEOUT, AMBARI_AGENT, sssdPayload, hosts)
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun setupRecipes(stack: Stack, hostGroups: Set<HostGroup>) {
        val instances = instanceMetadataRepository!!.findNotTerminatedForStack(stack.id)
        cleanupPlugins(stack, hostGroups, instances)
        uploadConsulRecipes(stack, getRecipesInHostGroups(hostGroups))
        setupProperties(stack, hostGroups)
        installPlugins(stack, hostGroups, instances)
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun cleanupPlugins(stack: Stack, hostGroups: Set<HostGroup>, instances: Set<InstanceMetaData>) {
        for (hostGroup in hostGroups) {
            LOGGER.info("Cleanup plugins on hostgroup {}.", hostGroup.name)
            cleanupPluginsOnHosts(stack, hostGroup.hostMetadata, instances)
        }
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun cleanupPluginsOnHosts(stack: Stack, hostMetadata: Set<HostMetadata>, instances: Set<InstanceMetaData>) {
        val gateway = stack.gatewayInstanceGroup
        val gatewayInstance = gateway.instanceMetaData.iterator().next()
        val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stack.id, gatewayInstance.publicIpWrapper)
        val eventIdMap = pluginManager!!.cleanupPlugins(clientConfig, getHostnames(hostMetadata))
        pluginManager.waitForEventFinish(stack, instances, eventIdMap, DEFAULT_RECIPE_TIMEOUT)
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun uploadConsulRecipes(stack: Stack, recipes: Iterable<Recipe>) {
        var clientConfig: HttpClientConfig? = null
        for (recipe in recipes) {
            for (plugin in recipe.plugins.keys) {
                if (plugin.startsWith("base64://")) {
                    val keyValues = HashMap<String, String>()
                    keyValues.put(getPluginConsulKey(recipe, plugin), String(Base64.decodeBase64(plugin.replaceFirst("base64://".toRegex(), ""))))
                    if (clientConfig == null) {
                        val gateway = stack.gatewayInstanceGroup
                        val gatewayInstance = gateway.instanceMetaData.iterator().next()
                        clientConfig = tlsSecurityService!!.buildTLSClientConfig(stack.id, gatewayInstance.publicIpWrapper)
                    }
                    pluginManager!!.prepareKeyValues(clientConfig, keyValues)
                }
            }
        }
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun setupProperties(stack: Stack, hostGroups: Set<HostGroup>) {
        LOGGER.info("Setting up recipe properties.")
        val gateway = stack.gatewayInstanceGroup
        val gatewayInstance = gateway.instanceMetaData.iterator().next()
        val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stack.id, gatewayInstance.publicIpWrapper)
        pluginManager!!.prepareKeyValues(clientConfig, getAllPropertiesFromRecipes(hostGroups))
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun installPlugins(stack: Stack, hostGroups: Set<HostGroup>, instances: Set<InstanceMetaData>) {
        for (hostGroup in hostGroups) {
            LOGGER.info("Installing plugins for recipes on hostgroup {}.", hostGroup.name)
            installPluginsOnHosts(stack, hostGroup.recipes, hostGroup.hostMetadata, instances, false)
        }
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun installPluginsOnHosts(stack: Stack, recipes: Set<Recipe>, hostMetadata: Set<HostMetadata>, instances: Set<InstanceMetaData>,
                                      existingHostGroup: Boolean) {
        val gateway = stack.gatewayInstanceGroup
        val gatewayInstance = gateway.instanceMetaData.iterator().next()
        val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stack.id, gatewayInstance.publicIpWrapper)
        for (recipe in recipes) {
            val plugins = HashMap<String, ExecutionType>()
            for (entry in recipe.plugins.entries) {
                val url = if (entry.key.startsWith("base64://")) "consul://" + getPluginConsulKey(recipe, entry.key) else entry.key
                plugins.put(url, entry.value)
            }
            val eventIdMap = pluginManager!!.installPlugins(clientConfig, plugins, getHostnames(hostMetadata), existingHostGroup)
            pluginManager.waitForEventFinish(stack, instances, eventIdMap, recipe.timeout)
        }
    }

    private fun getRecipesInHostGroups(hostGroups: Set<HostGroup>): Iterable<Recipe> {
        return Iterables.concat(Collections2.transform(hostGroups) { input -> input!!.recipes })
    }

    private fun getPluginConsulKey(recipe: Recipe, plugin: String): String {
        return RECIPE_KEY_PREFIX + recipe.name + plugin.hashCode()
    }

    @Throws(CloudbreakSecuritySetupException::class, CloudbreakRecipeSetupException::class)
    private fun executePreInstall(stack: Stack) {
        try {
            pluginManager!!.triggerAndWaitForPlugins(stack, ConsulPluginEvent.PRE_INSTALL, DEFAULT_RECIPE_TIMEOUT, AMBARI_AGENT)
        } catch (e: CloudbreakServiceException) {
            throw CloudbreakRecipeSetupException("Recipe pre install failed: " + e.message)
        }

    }

    @Throws(CloudbreakSecuritySetupException::class, CloudbreakRecipeSetupException::class)
    fun executePostInstall(stack: Stack) {
        try {
            pluginManager!!.triggerAndWaitForPlugins(stack, ConsulPluginEvent.POST_INSTALL, DEFAULT_RECIPE_TIMEOUT, AMBARI_AGENT)
        } catch (e: CloudbreakServiceException) {
            throw CloudbreakRecipeSetupException("Recipe post install failed: " + e.message)
        }

    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun executePostInstall(stack: Stack, hostMetadata: Set<HostMetadata>) {
        pluginManager!!.triggerAndWaitForPlugins(stack, ConsulPluginEvent.POST_INSTALL, DEFAULT_RECIPE_TIMEOUT, AMBARI_AGENT,
                emptyList<String>(), getHostnames(hostMetadata))
    }

    private fun getAllPropertiesFromRecipes(hostGroups: Set<HostGroup>): Map<String, String> {
        val properties = HashMap<String, String>()
        for (hostGroup in hostGroups) {
            for (recipe in hostGroup.recipes) {
                properties.putAll(recipe.keyValues)
            }
        }
        return properties
    }

    private fun getHostnames(hostMetadata: Set<HostMetadata>): Set<String> {
        return hostMetadata.stream().map(Function<HostMetadata, String> { it.hostName }).collect(Collectors.toSet<String>())
    }

    companion object {

        private val RECIPE_KEY_PREFIX = "consul-watch-plugin/"
        private val LOGGER = LoggerFactory.getLogger(ConsulRecipeExecutor::class.java)
    }
}
