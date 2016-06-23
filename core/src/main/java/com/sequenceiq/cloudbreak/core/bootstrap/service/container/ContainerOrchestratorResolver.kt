package com.sequenceiq.cloudbreak.core.bootstrap.service.container

import javax.annotation.Resource

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator

@Component
class ContainerOrchestratorResolver {

    @Resource
    private val containerOrchestrators: Map<String, ContainerOrchestrator>? = null

    @Throws(CloudbreakException::class)
    operator fun get(name: String): ContainerOrchestrator {
        val co = containerOrchestrators!![name]
        if (co == null) {
            LOGGER.error("ContainerOrchestrator not found: {}, supported ContainerOrchestrators: {}", name, containerOrchestrators)
            throw CloudbreakException("ContainerOrchestrator not found: " + name)
        }
        return co
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ContainerOrchestratorResolver::class.java)
    }

}
