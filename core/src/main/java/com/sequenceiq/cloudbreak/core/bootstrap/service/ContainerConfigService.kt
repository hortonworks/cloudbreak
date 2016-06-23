package com.sequenceiq.cloudbreak.core.bootstrap.service

import java.io.IOException

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.common.type.ComponentType
import com.sequenceiq.cloudbreak.core.bootstrap.config.ContainerConfigBuilder
import com.sequenceiq.cloudbreak.domain.Component
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.domain.json.Json
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig
import com.sequenceiq.cloudbreak.repository.ComponentRepository
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException

@Service
class ContainerConfigService {

    @Value("${cb.docker.container.ambari.agent:}")
    private val ambariAgent: String? = null

    @Value("${cb.docker.container.ambari.server:}")
    private val ambariServer: String? = null

    @Value("${cb.docker.container.registrator:}")
    private val registratorDockerImageName: String? = null

    @Value("${cb.docker.container.docker.consul.watch.plugn:}")
    private val consulWatchPlugnDockerImageName: String? = null

    @Value("${cb.docker.container.ambari.db:}")
    private val postgresDockerImageName: String? = null

    @Value("${cb.docker.container.kerberos:}")
    private val kerberosDockerImageName: String? = null

    @Value("${cb.docker.container.logrotate:}")
    private val logrotateDockerImageName: String? = null

    @Value("${cb.docker.container.munchausen:}")
    private val munchausenImageName: String? = null

    @Value("${cb.docker.container.haveged:}")
    private val havegedImageName: String? = null

    @Value("${cb.docker.container.ldap:}")
    private val ldapImageName: String? = null

    @Value("${cb.docker.container.shipyard:}")
    private val shipyardImageName: String? = null

    @Value("${cb.docker.container.shipyard.db:}")
    private val rethinkDbImageName: String? = null

    @Inject
    private val componentRepository: ComponentRepository? = null

    operator fun get(stack: Stack, dc: DockerContainer): ContainerConfig {
        try {
            var component: Component? = componentRepository!!.findComponentByStackIdComponentTypeName(stack.id, ComponentType.CONTAINER, dc.name)
            if (component == null) {
                component = create(stack, dc)
                LOGGER.info("Container component definition created: {}", component)
            } else {
                LOGGER.info("Container component definition found in database: {}", component)
            }
            return component.attributes.get<ContainerConfig>(ContainerConfig::class.java)
        } catch (e: IOException) {
            throw CloudbreakServiceException(String.format("Failed to parse component ContainerConfig for stack: %d, container: %s"))
        }

    }

    private fun create(stack: Stack, dc: DockerContainer): Component {
        try {
            val config: ContainerConfig
            when (dc) {
                DockerContainer.AMBARI_SERVER -> config = ContainerConfigBuilder.Builder(ambariServer).build()
                DockerContainer.AMBARI_AGENT -> config = ContainerConfigBuilder.Builder(ambariAgent).build()
                DockerContainer.AMBARI_DB -> config = ContainerConfigBuilder.Builder(postgresDockerImageName).build()
                DockerContainer.KERBEROS -> config = ContainerConfigBuilder.Builder(kerberosDockerImageName).build()
                DockerContainer.REGISTRATOR -> config = ContainerConfigBuilder.Builder(registratorDockerImageName).build()
                DockerContainer.MUNCHAUSEN -> config = ContainerConfigBuilder.Builder(munchausenImageName).build()
                DockerContainer.CONSUL_WATCH -> config = ContainerConfigBuilder.Builder(consulWatchPlugnDockerImageName).build()
                DockerContainer.LOGROTATE -> config = ContainerConfigBuilder.Builder(logrotateDockerImageName).build()
                DockerContainer.HAVEGED -> config = ContainerConfigBuilder.Builder(havegedImageName).build()
                DockerContainer.LDAP -> config = ContainerConfigBuilder.Builder(ldapImageName).build()
                DockerContainer.SHIPYARD -> config = ContainerConfigBuilder.Builder(shipyardImageName).build()
                DockerContainer.SHIPYARD_DB -> config = ContainerConfigBuilder.Builder(rethinkDbImageName).build()
                else -> throw CloudbreakServiceException(String.format("No configuration exist for %s", dc))
            }

            val component = Component(ComponentType.CONTAINER, dc.name, Json(config), stack)
            return componentRepository!!.save(component)
        } catch (e: IOException) {
            throw CloudbreakServiceException(String.format("Failed to parse component ContainerConfig for stack: %d, container: %s"))
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ContainerConfigService::class.java)
    }

}
