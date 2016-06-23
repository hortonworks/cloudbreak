package com.sequenceiq.cloudbreak.orchestrator.marathon

import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.MARATHON

import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.stream.Collectors

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component

import com.google.common.collect.Sets
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner
import com.sequenceiq.cloudbreak.orchestrator.container.SimpleContainerOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.marathon.poller.MarathonAppBootstrap
import com.sequenceiq.cloudbreak.orchestrator.marathon.poller.MarathonAppDeletion
import com.sequenceiq.cloudbreak.orchestrator.marathon.poller.MarathonTaskDeletion
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel

import mesosphere.marathon.client.Marathon
import mesosphere.marathon.client.MarathonClient
import mesosphere.marathon.client.model.v2.App
import mesosphere.marathon.client.model.v2.Container
import mesosphere.marathon.client.model.v2.Docker
import mesosphere.marathon.client.model.v2.GetAppResponse
import mesosphere.marathon.client.model.v2.Task
import mesosphere.marathon.client.utils.MarathonException

@Component
class MarathonContainerOrchestrator : SimpleContainerOrchestrator() {


    @Throws(CloudbreakOrchestratorException::class)
    override fun validateApiEndpoint(cred: OrchestrationCredential) {
        val client = MarathonClient.getInstance(cred.apiEndpoint)
        try {
            client.serverInfo
        } catch (e: Exception) {
            throw CloudbreakOrchestratorFailedException(e.message, e)
        }

    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun runContainer(config: ContainerConfig, cred: OrchestrationCredential, constraint: ContainerConstraint,
                              exitCriteriaModel: ExitCriteriaModel): List<ContainerInfo> {

        val image = config.name + ":" + config.version
        val name = constraint.containerName.getName().replace("_", "-")
        val appName = constraint.appName

        try {
            val result = ArrayList<ContainerInfo>()
            val client = MarathonClient.getInstance(cred.apiEndpoint)
            var app: App
            if (appName == null) {
                app = createMarathonApp(config, constraint, image, name)
                app = postAppToMarathon(config, client, app)
            } else {
                app = getMarathonApp(client, constraint.appName)
                app.instances = app.tasksRunning!! + constraint.instances!!
                app.constraints = constraint.getConstraints()
                updateApp(client, createMarathonUpdateApp(app))
            }

            val bootstrap = MarathonAppBootstrap(client, app)
            val runner = runner(bootstrap, exitCriteria, exitCriteriaModel)
            val appFuture = parallelOrchestratorComponentRunner!!.submit(runner)
            appFuture.get()

            val appResponse = client.getApp(app.id).app
            for (task in appResponse.tasks) {
                if (!isTaskFound(app, task)) {
                    result.add(ContainerInfo(task.id, appResponse.id, task.host, image))
                }
            }
            return result
        } catch (ex: Exception) {
            //To provide that the failed Marathon app and its deployment are not deleted from Marathon
            deleteApp(cred.apiEndpoint, name)
            val msg = String.format("Failed to create marathon app from image: '%s', with name: '%s'.", image, name)
            throw CloudbreakOrchestratorFailedException(msg, ex)
        }

    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun startContainer(info: List<ContainerInfo>, cred: OrchestrationCredential) {

    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun stopContainer(info: List<ContainerInfo>, cred: OrchestrationCredential) {

    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun deleteContainer(containerInfo: List<ContainerInfo>, cred: OrchestrationCredential) {
        try {
            val client = MarathonClient.getInstance(cred.apiEndpoint)
            val futures = ArrayList<Future<Boolean>>()

            val containersPerApp = HashMap<String, Set<String>>()
            for (info in containerInfo) {
                if (!containersPerApp.containsKey(info.name)) {
                    containersPerApp.put(info.name, Sets.newHashSet(info.id))
                } else {
                    containersPerApp[info.name].add(info.id)
                }
            }

            for (appName in containersPerApp.keys) {
                val app = client.getApp(appName).app
                val tasksInApp = app.tasks.stream().map(Function<Task, String> { it.getId() }).collect(Collectors.toSet<String>())
                if (containersPerApp[appName].containsAll(tasksInApp)) {
                    deleteEntireApp(client, futures, appName)
                } else {
                    deleteTasksFromApp(client, futures, containersPerApp, appName)
                }
            }

            for (future in futures) {
                future.get()
            }
        } catch (me: MarathonException) {
            val appNames = appNamesAsString(containerInfo)
            if (STATUS_NOT_FOUND == me.status) {
                LOGGER.warn("Failed to delete Marathon app it has been already deleted app ids: '{}'.", appNames)
            } else {
                val msg = String.format("Failed to delete Marathon app with app ids: '%s'.", appNames)
                throw CloudbreakOrchestratorFailedException(msg, me)
            }
        } catch (ex: Exception) {
            val appNames = appNamesAsString(containerInfo)
            val msg = String.format("Failed to delete Marathon app with app ids: '%s'.", appNames)
            throw CloudbreakOrchestratorFailedException(msg, ex)
        }

    }

    override fun getMissingNodes(gatewayConfig: GatewayConfig, nodes: Set<Node>): List<String> {
        return null
    }

    private fun appNamesAsString(containerInfo: List<ContainerInfo>): String {
        return Arrays.toString(containerInfo.stream().map(Function<ContainerInfo, String> { it.getName() }).toArray(String[]::new  /* Currently unsupported in Kotlin */))
    }

    override fun getAvailableNodes(gatewayConfig: GatewayConfig, nodes: Set<Node>): List<String> {
        return null
    }

    override fun name(): String {
        return MARATHON
    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun bootstrap(gatewayConfig: GatewayConfig, config: ContainerConfig, nodes: Set<Node>, consulServerCount: Int, exitCriteriaModel: ExitCriteriaModel) {
    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun bootstrapNewNodes(gatewayConfig: GatewayConfig, containerConfig: ContainerConfig, nodes: Set<Node>, exitCriteriaModel: ExitCriteriaModel) {
    }

    override fun isBootstrapApiAvailable(gatewayConfig: GatewayConfig): Boolean {
        return false
    }

    override val maxBootstrapNodes: Int
        get() = 0

    private fun createMarathonApp(config: ContainerConfig, constraint: ContainerConstraint, image: String, name: String): App {
        val app = App()
        app.id = name
        app.cpus = constraint.cpu ?: MIN_CPU
        app.setMem(constraint.mem ?: MIN_MEM)
        app.instances = constraint.instances ?: MIN_INSTANCES
        app.env = constraint.getEnv()
        app.constraints = constraint.getConstraints()

        val arrayOfCmd = constraint.cmd
        if (arrayOfCmd != null && arrayOfCmd.size > 0) {
            val sb = StringBuilder()
            for (cmd in arrayOfCmd) {
                sb.append(SPACE)
                sb.append(cmd)
            }
            app.cmd = sb.toString()
        }

        for (port in constraint.getPorts()) {
            app.addPort(port)
        }

        val docker = Docker()
        docker.isPrivileged = true
        docker.image = image
        docker.network = HOST_NETWORK_MODE

        val container = Container()
        container.type = DOCKER_CONTAINER_TYPE
        container.docker = docker
        app.container = container
        return app
    }

    @Throws(CloudbreakOrchestratorFailedException::class)
    private fun deleteApp(apiEndpoint: String, appName: String) {
        val futures = ArrayList<Future<Boolean>>()
        try {
            val client = MarathonClient.getInstance(apiEndpoint)
            deleteEntireApp(client, futures, appName)
            for (future in futures) {
                future.get()
            }
        } catch (e: Exception) {
            throw CloudbreakOrchestratorFailedException("Marathon app could not be deleted after creation error: ", e)
        }

    }

    @Throws(CloudbreakOrchestratorFailedException::class)
    private fun deleteEntireApp(client: Marathon, futures: MutableList<Future<Boolean>>, appName: String) {
        try {
            client.deleteApp(appName)
            val appDeletion = MarathonAppDeletion(client, appName)
            val runner = runner(appDeletion, exitCriteria, null)
            futures.add(parallelOrchestratorComponentRunner!!.submit(runner))
        } catch (me: MarathonException) {
            if (STATUS_NOT_FOUND == me.status) {
                LOGGER.info("Marathon app '{}' has already been deleted.", appName)
            } else {
                throw CloudbreakOrchestratorFailedException(me)
            }
        }

    }

    @Throws(CloudbreakOrchestratorFailedException::class)
    private fun deleteTasksFromApp(client: Marathon, futures: MutableList<Future<Boolean>>, containersPerApp: Map<String, Set<String>>, appName: String) {
        val taskIds = containersPerApp[appName]
        for (taskId in taskIds) {
            try {
                client.deleteAppTask(appName, taskId, "true")
                val taskDeletion = MarathonTaskDeletion(client, appName, taskIds)
                val runner = runner(taskDeletion, exitCriteria, null)
                futures.add(parallelOrchestratorComponentRunner!!.submit(runner))
            } catch (me: MarathonException) {
                if (STATUS_NOT_FOUND == me.status) {
                    LOGGER.info("Marathon task '{}' has already been deleted from app '{}'.", taskId, appName)
                } else {
                    throw CloudbreakOrchestratorFailedException(me)
                }
            }

        }
    }

    @Throws(CloudbreakOrchestratorFailedException::class)
    private fun getMarathonApp(client: Marathon, appId: String): App {
        try {
            val resp = client.getApp(appId)
            return resp.app
        } catch (e: MarathonException) {
            val msg = String.format("Failed to get Marathon app: %s", appId)
            LOGGER.error(msg, e)
            throw CloudbreakOrchestratorFailedException(msg, e)
        }

    }

    private fun createMarathonUpdateApp(appResponse: App): App {
        val app = App()
        app.id = appResponse.id
        app.instances = appResponse.instances
        return app
    }

    @Throws(CloudbreakOrchestratorFailedException::class)
    private fun postAppToMarathon(config: ContainerConfig, client: Marathon, app: App): App {
        try {
            return client.createApp(app)
        } catch (e: MarathonException) {
            val msg = String.format("Marathon container creation failed. From image: '%s', with name: '%s'!", config.name, app.id)
            LOGGER.error(msg, e)
            throw CloudbreakOrchestratorFailedException(msg, e)
        }

    }

    @Throws(CloudbreakOrchestratorFailedException::class)
    private fun updateApp(client: Marathon, app: App) {
        try {
            client.updateApp(app.id, app)
        } catch (e: MarathonException) {
            val msg = String.format("Failed to scale Marathon app %s to %s instances!", app.id, app.instances)
            LOGGER.error(msg, e)
            throw CloudbreakOrchestratorFailedException(msg, e)
        }

    }

    private fun isTaskFound(app: App, task: Task): Boolean {
        var taskFound = false
        if (app.tasks != null) {
            for (oldTask in app.tasks) {
                if (oldTask.id == task.id) {
                    taskFound = true
                    break
                }
            }
        }
        return taskFound
    }

    private fun runner(bootstrap: OrchestratorBootstrap, exitCriteria: ExitCriteria, exitCriteriaModel: ExitCriteriaModel?): Callable<Boolean> {
        return OrchestratorBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap())
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MarathonContainerOrchestrator::class.java)
        private val MIN_CPU = 0.5
        private val MIN_MEM = 1024
        private val MIN_INSTANCES = 1
        private val HOST_NETWORK_MODE = "HOST"
        private val DOCKER_CONTAINER_TYPE = "DOCKER"
        private val SPACE = " "
        private val STATUS_NOT_FOUND = 404
    }
}
