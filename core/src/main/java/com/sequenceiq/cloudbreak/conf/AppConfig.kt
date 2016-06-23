package com.sequenceiq.cloudbreak.conf

import java.io.File
import java.io.IOException
import java.security.Security
import java.util.ArrayList
import java.util.HashMap

import javax.annotation.PostConstruct
import javax.inject.Inject
import javax.inject.Named
import javax.ws.rs.client.Client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

import com.google.common.collect.Maps
import com.sequenceiq.cloudbreak.api.model.FileSystemType
import com.sequenceiq.cloudbreak.client.ConfigKey
import com.sequenceiq.cloudbreak.client.IdentityClient
import com.sequenceiq.cloudbreak.client.RestClientUtil
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptorMapFactory
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteria
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ExecutorBasedParallelOrchestratorComponentRunner
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator
import com.sequenceiq.cloudbreak.util.FileReaderUtils

@Configuration
class AppConfig : ResourceLoaderAware {

    @Value("#{'${cb.supported.container.orchestrators:}'.split(',')}")
    private val orchestrators: List<String>? = null

    @Value("${cb.threadpool.core.size:}")
    private val corePoolSize: Int = 0

    @Value("${cb.threadpool.capacity.size:}")
    private val queueCapacity: Int = 0

    @Value("${cb.intermediate.threadpool.core.size:}")
    private val intermediateCorePoolSize: Int = 0

    @Value("${cb.intermediate.threadpool.capacity.size:}")
    private val intermediateQueueCapacity: Int = 0

    @Value("${cb.container.threadpool.core.size:}")
    private val containerCorePoolSize: Int = 0

    @Value("${cb.container.threadpool.capacity.size:}")
    private val containerteQueueCapacity: Int = 0

    @Value("${cb.client.id}")
    private val clientId: String? = null

    @Value("${rest.debug:false}")
    private val restDebug: Boolean = false

    @Value("${cert.validation:true}")
    private val certificateValidation: Boolean = false

    @Inject
    private val containerOrchestrators: List<ContainerOrchestrator>? = null

    @Inject
    private val hostOrchestrators: List<HostOrchestrator>? = null

    @Inject
    private val fileSystemConfigurators: List<FileSystemConfigurator<FileSystemConfiguration>>? = null

    @Inject
    private val environment: ConfigurableEnvironment? = null

    @Inject
    @Named("identityServerUrl")
    private val identityServerUrl: String? = null

    private var resourceLoader: ResourceLoader? = null

    @PostConstruct
    @Throws(IOException::class)
    fun init() {
        Security.addProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())

        val patternResolver = PathMatchingResourcePatternResolver()
        val load = YamlPropertySourceLoader()
        for (resource in patternResolver.getResources("classpath*:*-images.yml")) {
            environment!!.propertySources.addLast(load.load(resource.filename, resource, null))
        }
        for (resource in loadEtcResources()) {
            environment!!.propertySources.addFirst(load.load(resource.filename, resource, null))
        }
    }

    @Bean
    fun clusterDeletionBasedExitCriteria(): ExitCriteria {
        return ClusterDeletionBasedExitCriteria()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun containerOrchestrators(): Map<String, ContainerOrchestrator> {
        val map = HashMap<String, ContainerOrchestrator>()
        for (containerOrchestrator in containerOrchestrators!!) {
            containerOrchestrator.init(simpleParallelContainerRunnerExecutor(), clusterDeletionBasedExitCriteria())
            map.put(containerOrchestrator.name(), containerOrchestrator)
        }
        return map
    }

    @Bean
    fun hostOrchestrators(): Map<String, HostOrchestrator> {
        val map = HashMap<String, HostOrchestrator>()
        for (hostOrchestrator in hostOrchestrators!!) {
            hostOrchestrator.init(simpleParallelContainerRunnerExecutor(), clusterDeletionBasedExitCriteria())
            map.put(hostOrchestrator.name(), hostOrchestrator)
        }
        return map
    }

    @Bean
    fun simpleParallelContainerRunnerExecutor(): ParallelOrchestratorComponentRunner {
        return ExecutorBasedParallelOrchestratorComponentRunner(containerBootstrapBuilderExecutor())
    }

    @Bean
    fun fileSystemConfigurators(): Map<FileSystemType, FileSystemConfigurator<FileSystemConfiguration>> {
        val map = HashMap<FileSystemType, FileSystemConfigurator<FileSystemConfiguration>>()
        for (fileSystemConfigurator in fileSystemConfigurators!!) {
            map.put(fileSystemConfigurator.fileSystemType, fileSystemConfigurator)
        }
        return map
    }

    @Bean
    fun intermediateBuilderExecutor(): AsyncTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = intermediateCorePoolSize
        executor.setQueueCapacity(intermediateQueueCapacity)
        executor.threadNamePrefix = "intermediateBuilderExecutor-"
        executor.initialize()
        return executor
    }

    @Bean
    fun resourceBuilderExecutor(): AsyncTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = corePoolSize
        executor.setQueueCapacity(queueCapacity)
        executor.threadNamePrefix = "resourceBuilderExecutor-"
        executor.initialize()
        return executor
    }

    @Bean
    fun containerBootstrapBuilderExecutor(): AsyncTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = containerCorePoolSize
        executor.setQueueCapacity(containerteQueueCapacity)
        executor.threadNamePrefix = "containerBootstrapBuilderExecutor-"
        executor.initialize()
        return executor
    }

    @Bean
    fun identityClient(): IdentityClient {
        return IdentityClient(identityServerUrl, clientId, ConfigKey(certificateValidation, restDebug))
    }

    @Bean
    fun restClient(): Client {
        return RestClientUtil[ConfigKey(certificateValidation, restDebug)]
    }

    @Bean
    @Throws(IOException::class)
    fun stackServiceComponentDescriptorMapFactory(): StackServiceComponentDescriptorMapFactory {
        val minCardinalityReps = Maps.newHashMap<String, Int>()
        minCardinalityReps.put("1", 1)
        minCardinalityReps.put("0-1", 0)
        minCardinalityReps.put("1-2", 1)
        minCardinalityReps.put("0+", 0)
        minCardinalityReps.put("1+", 1)
        minCardinalityReps.put("ALL", 0)
        val maxCardinalityReps = Maps.newHashMap<String, Int>()
        maxCardinalityReps.put("1", 1)
        maxCardinalityReps.put("0-1", 1)
        maxCardinalityReps.put("1-2", 2)
        maxCardinalityReps.put("0+", Integer.MAX_VALUE)
        maxCardinalityReps.put("1+", Integer.MAX_VALUE)
        maxCardinalityReps.put("ALL", Integer.MAX_VALUE)
        val stackServiceComponentsJson = FileReaderUtils.readFileFromClasspath("hdp/hdp-services.json")
        return StackServiceComponentDescriptorMapFactory(stackServiceComponentsJson, minCardinalityReps, maxCardinalityReps)
    }

    private fun loadEtcResources(): List<Resource> {
        val folder = File(ETC_DIR)
        val listOfFiles = folder.listFiles()
        val resources = ArrayList<Resource>()
        if (listOfFiles != null) {
            for (file in listOfFiles) {
                try {
                    if (file.isFile && file.name.endsWith("yml") || file.name.endsWith("yaml")) {
                        resources.add(resourceLoader!!.getResource("file:" + file.absolutePath))
                    }
                } catch (e: Exception) {
                    LOGGER.warn("Cannot load file into property source: {}", file.absolutePath)
                }

            }
        }
        return resources
    }

    override fun setResourceLoader(resourceLoader: ResourceLoader) {
        this.resourceLoader = resourceLoader
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AppConfig::class.java)
        private val ETC_DIR = "/etc/cloudbreak"
    }
}
