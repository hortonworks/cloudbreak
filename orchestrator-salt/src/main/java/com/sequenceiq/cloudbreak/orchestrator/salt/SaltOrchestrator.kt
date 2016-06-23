package com.sequenceiq.cloudbreak.orchestrator.salt

import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SALT

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.TreeMap
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

import com.google.common.collect.Sets
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeExecutionPhase
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarConfig
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.PillarSave
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltBootstrap
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltCommandTracker
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobIdTracker
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainAddRunner
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainRemoveRunner
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.HighStateRunner
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.SyncGrainsRunner
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel

@Component
class SaltOrchestrator : HostOrchestrator {

    @Value("${rest.debug:false}")
    private val restDebug: Boolean = false

    @Value("${cb.smartsense.configure:false}")
    private val configureSmartSense: Boolean = false

    private var parallelOrchestratorComponentRunner: ParallelOrchestratorComponentRunner? = null
    private var exitCriteria: ExitCriteria? = null

    override fun init(parallelOrchestratorComponentRunner: ParallelOrchestratorComponentRunner, exitCriteria: ExitCriteria) {
        this.parallelOrchestratorComponentRunner = parallelOrchestratorComponentRunner
        this.exitCriteria = exitCriteria
    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun bootstrap(gatewayConfig: GatewayConfig, targets: Set<Node>, consulServerCount: Int, exitCriteriaModel: ExitCriteriaModel) {
        try {
            SaltConnector(gatewayConfig, restDebug).use { sc ->
                uploadSaltConfig(sc)

                val ambariServer = PillarSave(sc, gatewayConfig.privateAddress)
                val saltPillarRunner = runner(ambariServer, exitCriteria, exitCriteriaModel)
                val saltPillarRunnerFuture = parallelOrchestratorComponentRunner.submit(saltPillarRunner)
                saltPillarRunnerFuture.get()

                val saltBootstrap = SaltBootstrap(sc, gatewayConfig, targets)
                val saltBootstrapRunner = runner(saltBootstrap, exitCriteria, exitCriteriaModel)
                val saltBootstrapRunnerFuture = parallelOrchestratorComponentRunner.submit(saltBootstrapRunner)
                saltBootstrapRunnerFuture.get()
            }
        } catch (e: Exception) {
            LOGGER.error("Error occurred under the consul bootstrap", e)
            throw CloudbreakOrchestratorFailedException(e)
        }

    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun bootstrapNewNodes(gatewayConfig: GatewayConfig, targets: Set<Node>, exitCriteriaModel: ExitCriteriaModel) {
        try {
            SaltConnector(gatewayConfig, restDebug).use { sc ->
                val saltBootstrap = SaltBootstrap(sc, gatewayConfig, targets)
                val saltBootstrapRunner = runner(saltBootstrap, exitCriteria, exitCriteriaModel)
                val saltBootstrapRunnerFuture = parallelOrchestratorComponentRunner.submit(saltBootstrapRunner)
                saltBootstrapRunnerFuture.get()
            }
        } catch (e: Exception) {
            LOGGER.error("Error occurred during salt upscale", e)
            throw CloudbreakOrchestratorFailedException(e)
        }

    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun runService(gatewayConfig: GatewayConfig, allNodes: Set<Node>, pillarConfig: SaltPillarConfig,
                            exitCriteriaModel: ExitCriteriaModel) {
        try {
            SaltConnector(gatewayConfig, restDebug).use { sc ->
                val hostSave = PillarSave(sc, allNodes)
                var saltPillarRunner = runner(hostSave, exitCriteria, exitCriteriaModel)
                var saltPillarRunnerFuture = parallelOrchestratorComponentRunner.submit(saltPillarRunner)
                saltPillarRunnerFuture.get()

                for (propertiesEntry in pillarConfig.servicePillarConfig!!.entries) {
                    val pillarSave = PillarSave(sc, propertiesEntry.value)
                    saltPillarRunner = runner(pillarSave, exitCriteria, exitCriteriaModel)
                    saltPillarRunnerFuture = parallelOrchestratorComponentRunner.submit(saltPillarRunner)
                    saltPillarRunnerFuture.get()
                }

                val server = Sets.newHashSet(gatewayConfig.privateAddress)
                val all = allNodes.stream().map(Function<Node, String> { it.getPrivateIp() }).collect(Collectors.toSet<String>())

                LOGGER.info("Pillar saved, starting to set up discovery...")
                //run discovery only
                runNewService(sc, HighStateRunner(all, allNodes), exitCriteriaModel)

                LOGGER.info("Pillar saved, discovery has been set up with highstate")

                // ambari server
                runSaltCommand(sc, GrainAddRunner(server, allNodes, "ambari_server"), exitCriteriaModel)
                // ambari agent
                runSaltCommand(sc, GrainAddRunner(all, allNodes, "ambari_agent"), exitCriteriaModel)
                // kerberos
                if (pillarConfig.servicePillarConfig!!.containsKey("kerberos")) {
                    runSaltCommand(sc, GrainAddRunner(server, allNodes, "kerberos_server"), exitCriteriaModel)
                }
                if (configureSmartSense) {
                    runSaltCommand(sc, GrainAddRunner(all, allNodes, "smartsense"), exitCriteriaModel)
                    runSaltCommand(sc, GrainAddRunner(server, allNodes, "smartsense_gateway"), exitCriteriaModel)
                }
                runSaltCommand(sc, SyncGrainsRunner(all, allNodes), exitCriteriaModel)
                runNewService(sc, HighStateRunner(all, allNodes), exitCriteriaModel)
            }
        } catch (e: Exception) {
            LOGGER.error("Error occurred during ambari bootstrap", e)
            throw CloudbreakOrchestratorFailedException(e)
        }

    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun resetAmbari(gatewayConfig: GatewayConfig, target: Set<String>, allNodes: Set<Node>, exitCriteriaModel: ExitCriteriaModel) {
        try {
            SaltConnector(gatewayConfig, restDebug).use { saltConnector ->
                val baseSaltJobRunner = object : BaseSaltJobRunner(target, allNodes) {
                    override fun submit(saltConnector: SaltConnector): String {
                        return SaltStates.ambariReset(saltConnector, Compound(target, Compound.CompoundType.HOST))
                    }
                }
                val saltJobIdTracker = SaltJobIdTracker(saltConnector, baseSaltJobRunner)
                val saltJobRunBootstrapRunner = runner(saltJobIdTracker, exitCriteria, exitCriteriaModel)
                val saltJobRunBootstrapFuture = parallelOrchestratorComponentRunner.submit(saltJobRunBootstrapRunner)
                saltJobRunBootstrapFuture.get()
            }
        } catch (e: Exception) {
            LOGGER.error("Error occurred during reset", e)
            throw CloudbreakOrchestratorFailedException(e)
        }

    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun tearDown(gatewayConfig: GatewayConfig, hostnames: List<String>) {
        try {
            SaltConnector(gatewayConfig, restDebug).use { saltConnector -> SaltStates.removeMinions(saltConnector, hostnames) }
        } catch (e: Exception) {
            LOGGER.error("Error occurred during salt minion tear down", e)
            throw CloudbreakOrchestratorFailedException(e)
        }

    }

    @Throws(CloudbreakOrchestratorFailedException::class)
    fun uploadRecipes(gatewayConfig: GatewayConfig, recipes: Map<String, List<RecipeModel>>, allNodes: Set<Node>, exitCriteriaModel: ExitCriteriaModel) {
        try {
            SaltConnector(gatewayConfig, restDebug).use { sc ->
                val scriptPillarSave = PillarSave(sc, recipes)
                val saltPillarRunner = runner(scriptPillarSave, exitCriteria, exitCriteriaModel)
                val saltPillarRunnerFuture = parallelOrchestratorComponentRunner.submit(saltPillarRunner)
                saltPillarRunnerFuture.get()

                for (recipeList in recipes.values) {
                    for (model in recipeList) {
                        if (model.preInstall != null) {
                            uploadRecipe(sc, model.name, model.preInstall, RecipeExecutionPhase.PRE)
                        }
                        if (model.postInstall != null) {
                            uploadRecipe(sc, model.name, model.postInstall, RecipeExecutionPhase.POST)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error occurred during recipe upload", e)
            throw CloudbreakOrchestratorFailedException(e)
        }

    }

    @Throws(CloudbreakOrchestratorFailedException::class)
    override fun preInstallRecipes(gatewayConfig: GatewayConfig, allNodes: Set<Node>, exitCriteriaModel: ExitCriteriaModel) {
        executeRecipes(gatewayConfig, allNodes, exitCriteriaModel, RecipeExecutionPhase.PRE)
    }

    @Throws(CloudbreakOrchestratorFailedException::class)
    override fun postInstallRecipes(gatewayConfig: GatewayConfig, allNodes: Set<Node>, exitCriteriaModel: ExitCriteriaModel) {
        executeRecipes(gatewayConfig, allNodes, exitCriteriaModel, RecipeExecutionPhase.POST)
    }

    override fun getMissingNodes(gatewayConfig: GatewayConfig, nodes: Set<Node>): List<String> {
        return ArrayList()
    }

    override fun getAvailableNodes(gatewayConfig: GatewayConfig, nodes: Set<Node>): List<String> {
        return ArrayList()
    }

    override fun isBootstrapApiAvailable(gatewayConfig: GatewayConfig): Boolean {
        val saltConnector = SaltConnector(gatewayConfig, restDebug)
        try {
            if (saltConnector.health().statusCode == HttpStatus.OK.value()) {
                return true
            }
        } catch (e: Exception) {
            LOGGER.info("Failed to connect to bootstrap app {}", e.message)
        }

        return false
    }

    override fun name(): String {
        return SALT
    }

    override val maxBootstrapNodes: Int
        get() = MAX_NODES

    @Throws(ExecutionException::class, InterruptedException::class)
    private fun runNewService(sc: SaltConnector, baseSaltJobRunner: BaseSaltJobRunner, exitCriteriaModel: ExitCriteriaModel) {
        val saltJobIdTracker = SaltJobIdTracker(sc, baseSaltJobRunner)
        val saltJobRunBootstrapRunner = runner(saltJobIdTracker, exitCriteria, exitCriteriaModel)
        val saltJobRunBootstrapFuture = parallelOrchestratorComponentRunner.submit(saltJobRunBootstrapRunner)
        saltJobRunBootstrapFuture.get()
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    private fun runSaltCommand(sc: SaltConnector, baseSaltJobRunner: BaseSaltJobRunner, exitCriteriaModel: ExitCriteriaModel) {
        val saltCommandTracker = SaltCommandTracker(sc, baseSaltJobRunner)
        val saltCommandRunBootstrapRunner = runner(saltCommandTracker, exitCriteria, exitCriteriaModel)
        val saltCommandRunBootstrapFuture = parallelOrchestratorComponentRunner.submit(saltCommandRunBootstrapRunner)
        saltCommandRunBootstrapFuture.get()
    }

    @Throws(CloudbreakOrchestratorFailedException::class)
    private fun executeRecipes(gatewayConfig: GatewayConfig, allNodes: Set<Node>, exitCriteriaModel: ExitCriteriaModel, phase: RecipeExecutionPhase) {
        try {
            SaltConnector(gatewayConfig, restDebug).use { sc ->
                // add 'recipe' grain to all nodes
                var targets = allNodes.stream().map(Function<Node, String> { it.getPrivateIp() }).collect(Collectors.toSet<String>())
                runSaltCommand(sc, GrainAddRunner(targets, allNodes, "recipes", phase.value(), Compound.CompoundType.IP), exitCriteriaModel)

                val all = allNodes.stream().map(Function<Node, String> { it.getPrivateIp() }).collect(Collectors.toSet<String>())
                runSaltCommand(sc, SyncGrainsRunner(all, allNodes), exitCriteriaModel)
                runNewService(sc, HighStateRunner(all, allNodes), exitCriteriaModel)

                // remove 'recipe' grain from all nodes
                targets = allNodes.stream().map(Function<Node, String> { it.getPrivateIp() }).collect(Collectors.toSet<String>())
                runSaltCommand(sc, GrainRemoveRunner(targets, allNodes, "recipes", phase.value(), Compound.CompoundType.IP), exitCriteriaModel)
            }
        } catch (e: Exception) {
            LOGGER.error("Error occurred during recipe execution", e)
            throw CloudbreakOrchestratorFailedException(e)
        }

    }

    private fun runner(bootstrap: OrchestratorBootstrap, exitCriteria: ExitCriteria, exitCriteriaModel: ExitCriteriaModel): Callable<Boolean> {
        return OrchestratorBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap(), MAX_RETRY_COUNT, SLEEP_TIME)
    }

    @Throws(IOException::class)
    private fun uploadSaltConfig(saltConnector: SaltConnector) {
        val byteArray = zipSaltConfig()
        LOGGER.info("Upload salt.zip to /tmp/salt.zip")
        saltConnector.upload("/srv", "salt.zip", ByteArrayInputStream(byteArray))
    }

    @Throws(IOException::class)
    private fun zipSaltConfig(): ByteArray {
        val baos = ByteArrayOutputStream()
        try {
            val zout = ZipOutputStream(baos)
            val resolver = PathMatchingResourcePatternResolver()
            val structure = TreeMap<String, List<Resource>>()
            for (resource in resolver.getResources("classpath*:salt/**")) {
                val path = resource.url.path
                val dir = path.substring(path.indexOf("/salt") + "/salt".length, path.lastIndexOf("/") + 1)
                var list: MutableList<Resource>? = structure[dir]
                if (list == null) {
                    list = ArrayList<Resource>()
                }
                structure.put(dir, list)
                if (!path.endsWith("/")) {
                    list.add(resource)
                }
            }
            for (dir in structure.keys) {
                zout.putNextEntry(ZipEntry(dir))
                for (resource in structure[dir]) {
                    LOGGER.info("Zip salt entry: {}", resource.getFilename())
                    zout.putNextEntry(ZipEntry(dir + resource.getFilename()))
                    val inputStream = resource.getInputStream()
                    val bytes = IOUtils.toByteArray(inputStream)
                    zout.write(bytes)
                    zout.closeEntry()
                }
            }
            zout.close()
            baos.close()
        } catch (e: IOException) {
            LOGGER.error("Failed to zip salt configurations", e)
            throw IOException("Failed to zip salt configurations", e)
        }

        return baos.toByteArray()
    }

    private fun uploadRecipe(sc: SaltConnector, name: String, recipe: String, phase: RecipeExecutionPhase) {
        val recipeBytes = recipe.toByteArray(StandardCharsets.UTF_8)
        LOGGER.info("Upload '{}' recipe: {}", phase.value(), name)
        try {
            if (RecipeExecutionPhase.PRE == phase) {
                sc.upload("/srv/salt/pre-recipes/scripts", name, ByteArrayInputStream(recipeBytes))
            } else {
                sc.upload("/srv/salt/post-recipes/scripts", name, ByteArrayInputStream(recipeBytes))
            }
        } catch (e: IOException) {
            LOGGER.warn("Cannot upload recipe: {}", recipe)
        }

    }

    companion object {

        private val MAX_NODES = 5000
        private val MAX_RETRY_COUNT = 60
        private val SLEEP_TIME = 10000

        private val LOGGER = LoggerFactory.getLogger(SaltOrchestrator::class.java)
    }
}
