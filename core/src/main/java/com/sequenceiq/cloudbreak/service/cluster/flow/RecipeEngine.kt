package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SALT
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SWARM
import java.util.Arrays.asList

import java.io.IOException
import java.util.Arrays
import java.util.Collections
import java.util.HashMap

import javax.annotation.Resource
import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.google.common.collect.Sets
import com.sequenceiq.cloudbreak.api.model.ExecutionType
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration
import com.sequenceiq.cloudbreak.api.model.FileSystemType
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.FileSystem
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Recipe
import com.sequenceiq.cloudbreak.domain.SssdConfig
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintProcessor
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.SmartSenseConfigProvider
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig
import com.sequenceiq.cloudbreak.util.FileReaderUtils
import com.sequenceiq.cloudbreak.util.JsonUtil

@Component
class RecipeEngine {

    @Inject
    private val pluginManager: PluginManager? = null
    @Inject
    private val tlsSecurityService: TlsSecurityService? = null
    @Inject
    private val orchestratorTypeResolver: OrchestratorTypeResolver? = null
    @Resource
    private val fileSystemConfigurators: Map<FileSystemType, FileSystemConfigurator<FileSystemConfiguration>>? = null
    @Inject
    private val recipeBuilder: RecipeBuilder? = null
    @Inject
    private val consulRecipeExecutor: ConsulRecipeExecutor? = null
    @Inject
    private val orchestratorRecipeExecutor: OrchestratorRecipeExecutor? = null
    @Inject
    private val blueprintProcessor: BlueprintProcessor? = null
    @Inject
    private val smartSenseConfigProvider: SmartSenseConfigProvider? = null

    @Throws(CloudbreakException::class)
    fun executePreInstall(stack: Stack, hostGroups: Set<HostGroup>) {
        configureSssd(stack, null)
        addFsRecipes(stack, hostGroups)
        addSmartSenseRecipe(stack, hostGroups)
        val recipesFound = recipesFound(hostGroups)
        if (recipesFound) {
            val orchestrator = stack.orchestrator.type
            val orchestratorType = orchestratorTypeResolver!!.resolveType(orchestrator)
            if (orchestratorType.containerOrchestrator()) {
                consulRecipeExecutor!!.preInstall(stack, hostGroups)
            } else {
                orchestratorRecipeExecutor!!.uploadRecipes(stack, hostGroups)
                orchestratorRecipeExecutor.preInstall(stack)
            }
        }
    }

    @Throws(CloudbreakException::class)
    fun executeUpscalePreInstall(stack: Stack, hostGroup: HostGroup, metaData: Set<HostMetadata>) {
        val hostGroups = setOf<HostGroup>(hostGroup)
        configureSssd(stack, metaData)
        addFsRecipes(stack, hostGroups)
        val recipesFound = recipesFound(hostGroups)
        if (recipesFound) {
            val orchestrator = stack.orchestrator.type
            val orchestratorType = orchestratorTypeResolver!!.resolveType(orchestrator)
            if (orchestratorType.containerOrchestrator()) {
                consulRecipeExecutor!!.setupRecipesOnHosts(stack, hostGroup.recipes, metaData)
                consulRecipeExecutor.executePreInstall(stack, metaData)
            } else {
                orchestratorRecipeExecutor!!.preInstall(stack)
            }
        }
    }

    @Throws(CloudbreakException::class)
    fun executePostInstall(stack: Stack) {
        val orchestrator = stack.orchestrator.type
        val orchestratorType = orchestratorTypeResolver!!.resolveType(orchestrator)
        if (orchestratorType.containerOrchestrator()) {
            consulRecipeExecutor!!.executePostInstall(stack)
        } else {
            orchestratorRecipeExecutor!!.postInstall(stack)
        }
    }

    @Throws(CloudbreakException::class)
    fun executeUpscalePostInstall(stack: Stack, hostMetadata: Set<HostMetadata>) {
        val orchestrator = stack.orchestrator.type
        val orchestratorType = orchestratorTypeResolver!!.resolveType(orchestrator)
        if (orchestratorType.containerOrchestrator()) {
            consulRecipeExecutor!!.executePostInstall(stack, hostMetadata)
        } else {
            orchestratorRecipeExecutor!!.postInstall(stack)
        }
    }

    @Throws(CloudbreakException::class)
    private fun addFsRecipes(stack: Stack, hostGroups: Set<HostGroup>) {
        val orchestrator = stack.orchestrator.type
        if (SWARM == orchestrator || SALT == orchestrator) {
            val cluster = stack.cluster
            val blueprintText = cluster.blueprint.blueprintText
            val fs = cluster.fileSystem
            if (fs != null) {
                try {
                    addFsRecipesToHostGroups(hostGroups, blueprintText, fs)
                } catch (e: IOException) {
                    throw CloudbreakException("can not add FS recipes to host groups", e)
                }

            }
            addHDFSRecipe(cluster, blueprintText, hostGroups)
        }
    }

    @Throws(IOException::class)
    private fun addFsRecipesToHostGroups(hostGroups: Set<HostGroup>, blueprintText: String, fs: FileSystem) {
        val fsConfigurator = fileSystemConfigurators!![FileSystemType.valueOf(fs.type)]
        val fsConfiguration = getFileSystemConfiguration(fs)
        val recipeScripts = fsConfigurator.getScripts(fsConfiguration)
        val fsRecipes = recipeBuilder!!.buildRecipes(recipeScripts, fs.properties)
        for (recipe in fsRecipes) {
            var oneNode = false
            for (pluginEntries in recipe.plugins.entries) {
                if (ExecutionType.ONE_NODE == pluginEntries.value) {
                    oneNode = true
                }
            }
            if (oneNode) {
                for (hostGroup in hostGroups) {
                    if (isComponentPresent(blueprintText, "NAMENODE", hostGroup)) {
                        hostGroup.addRecipe(recipe)
                        break
                    }
                }
            } else {
                for (hostGroup in hostGroups) {
                    hostGroup.addRecipe(recipe)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun getFileSystemConfiguration(fs: FileSystem): FileSystemConfiguration {
        val json = JsonUtil.writeValueAsString(fs.properties)
        return JsonUtil.readValue<FileSystemConfiguration>(json, FileSystemConfiguration::class.java)
    }

    @Throws(CloudbreakException::class)
    private fun configureSssd(stack: Stack, hostMetadata: Set<HostMetadata>?) {
        if (stack.cluster.sssdConfig != null) {
            val sssdPayload = generateSssdRecipePayload(stack)
            val orchestrator = stack.orchestrator.type
            val orchestratorType = orchestratorTypeResolver!!.resolveType(orchestrator)
            if (orchestratorType.containerOrchestrator()) {
                consulRecipeExecutor!!.configureSssd(stack, hostMetadata, sssdPayload)
            } // TODO hostOrchestrator
        }
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun generateSssdRecipePayload(stack: Stack): List<String> {
        val config = stack.cluster.sssdConfig
        val payload: List<String>
        if (config.configuration != null) {
            val keyValues = HashMap<String, String>()
            val configName = SSSD_CONFIG + config.id!!
            keyValues.put(configName, config.configuration)
            val gateway = stack.gatewayInstanceGroup
            val gatewayInstance = gateway.instanceMetaData.iterator().next()
            val httpClientConfig = tlsSecurityService!!.buildTLSClientConfig(stack.id, gatewayInstance.publicIpWrapper)
            pluginManager!!.prepareKeyValues(httpClientConfig, keyValues)
            payload = Arrays.asList(configName)
        } else {
            payload = Arrays.asList("-", config.providerType.type, config.url, config.schema.representation,
                    config.baseSearch, config.tlsReqcert.representation, config.adServer,
                    config.kerberosServer, config.kerberosRealm)
        }
        return payload
    }

    private fun recipesFound(hostGroups: Set<HostGroup>): Boolean {
        for (hostGroup in hostGroups) {
            if (!hostGroup.recipes.isEmpty()) {
                return true
            }
        }
        return false
    }

    private fun addHDFSRecipe(cluster: Cluster, blueprintText: String, hostGroups: Set<HostGroup>) {
        try {
            for (hostGroup in hostGroups) {
                if (isComponentPresent(blueprintText, "NAMENODE", hostGroup)) {
                    val script = FileReaderUtils.readFileFromClasspath("scripts/hdfs-home.sh").replace("\\$USER".toRegex(), cluster.userName)
                    val recipeScript = RecipeScript(script, ClusterLifecycleEvent.POST_INSTALL, ExecutionType.ONE_NODE)
                    val recipe = recipeBuilder!!.buildRecipes(asList(recipeScript), emptyMap<String, String>())[0]
                    hostGroup.addRecipe(recipe)
                    break
                }
            }
        } catch (e: IOException) {
            LOGGER.warn("Cannot create HDFS home dir recipe", e)
        }

    }

    private fun addSmartSenseRecipe(stack: Stack, hostGroups: Set<HostGroup>) {
        try {
            val cluster = stack.cluster
            val blueprintText = cluster.blueprint.blueprintText
            if (smartSenseConfigProvider!!.smartSenseIsConfigurable(blueprintText)) {
                for (hostGroup in hostGroups) {
                    if (isComponentPresent(blueprintText, "HST_AGENT", hostGroup)) {
                        val script = FileReaderUtils.readFileFromClasspath("scripts/smartsense-capture-schedule.sh")
                        val recipeScript = RecipeScript(script, ClusterLifecycleEvent.POST_INSTALL, ExecutionType.ONE_NODE)
                        val recipe = recipeBuilder!!.buildRecipes(asList(recipeScript), emptyMap<String, String>())[0]
                        hostGroup.addRecipe(recipe)
                        break
                    }
                }
            }
        } catch (e: IOException) {
            LOGGER.warn("Cannot create SmartSense caputre schedule setter recipe", e)
        }

    }

    private fun isComponentPresent(blueprint: String, component: String, hostGroup: HostGroup): Boolean {
        return isComponentPresent(blueprint, component, Sets.newHashSet(hostGroup))
    }

    private fun isComponentPresent(blueprint: String, component: String, hostGroups: Set<HostGroup>): Boolean {
        for (hostGroup in hostGroups) {
            val components = blueprintProcessor!!.getComponentsInHostGroup(blueprint, hostGroup.name)
            if (components.contains(component)) {
                return true
            }
        }
        return false
    }

    companion object {

        val DEFAULT_RECIPE_TIMEOUT = 15
        private val SSSD_CONFIG = "sssd-config-"
        private val LOGGER = LoggerFactory.getLogger(RecipeEngine::class.java)
    }

}
