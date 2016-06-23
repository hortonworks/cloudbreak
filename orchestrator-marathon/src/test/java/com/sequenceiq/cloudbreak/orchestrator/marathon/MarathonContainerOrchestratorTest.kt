package com.sequenceiq.cloudbreak.orchestrator.marathon

import com.google.common.collect.ImmutableMap
import mesosphere.marathon.client.Marathon
import mesosphere.marathon.client.MarathonClient
import mesosphere.marathon.client.model.v2.App
import mesosphere.marathon.client.model.v2.Container
import mesosphere.marathon.client.model.v2.Docker
import mesosphere.marathon.client.utils.MarathonException
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(MockitoJUnitRunner::class)
@Ignore
class MarathonContainerOrchestratorTest {

    private var client: Marathon? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        client = MarathonClient.getInstance("http://172.16.252.31:8080")
    }

    @Test
    fun testCreateAmbariServerDbContainer() {
        var ambariDb = App()
        ambariDb.id = "ambari-db"
        ambariDb.cpus = 0.5
        ambariDb.mem = 512.0
        ambariDb.instances = 1

        val dbContainer = Container()
        dbContainer.type = "DOCKER"

        val dbDocker = Docker()
        dbDocker.image = "postgres:9.4.1"
        dbDocker.network = "HOST"
        dbContainer.docker = dbDocker

        ambariDb.env = ImmutableMap.of("POSTGRES_PASSWORD", "bigdata", "POSTGRES_USER", "ambari")
        ambariDb.container = dbContainer

        try {
            ambariDb = client!!.createApp(ambariDb)
            LOGGER.info(ambariDb.toString())
        } catch (e: MarathonException) {
            LOGGER.error("App could not be created on Marathon: ", e)
        }

    }

    @Test
    fun testCreateAmbariServerContainer() {

        val dbHost = "mesos-slave2"

        var server = App()
        server.id = "ambari-server3"
        server.cpus = 1.5
        server.mem = 4096.0
        server.instances = 1
        server.addPort(8080)

        val serverContainer = Container()
        serverContainer.type = "DOCKER"

        val serverDocker = Docker()
        serverDocker.isPrivileged = true
        serverDocker.image = "hortonworks/ambari-server:2.2.1-v5"
        serverDocker.network = "HOST"
        serverContainer.docker = serverDocker

        server.cmd = String.format("/usr/sbin/init systemd.setenv=POSTGRES_DB=%s", dbHost)
        server.container = serverContainer

        try {
            server = client!!.createApp(server)
            LOGGER.info(server.toString())
        } catch (e: MarathonException) {
            LOGGER.error("App could not be created on Marathon: ", e)
        }

    }

    @Test
    fun testCreateAmbariAgentsContainer() {

        val serverHost = "mesos-slave1"

        var agents = App()
        agents.id = "ambari-agent"
        agents.cpus = 1.5
        agents.mem = 4096.0
        agents.instances = 3

        val agentContainer = Container()
        agentContainer.type = "DOCKER"

        val agentDocker = Docker()
        agentDocker.isPrivileged = true
        agentDocker.image = "hortonworks/ambari-agent:2.2.1-v5"
        agentDocker.network = "HOST"
        agentContainer.docker = agentDocker

        agents.cmd = String.format("/usr/sbin/init systemd.setenv=AMBARI_SERVER_ADDR=%s systemd.setenv=USE_CONSUL_DNS=false", serverHost)
        agents.container = agentContainer

        try {
            agents = client!!.createApp(agents)
            LOGGER.info(agents.toString())
        } catch (e: MarathonException) {
            LOGGER.error("App could not be created on Marathon: ", e)
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MarathonContainerOrchestratorTest::class.java)
    }
}