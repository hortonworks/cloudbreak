package com.sequenceiq.cloudbreak.core.bootstrap.service

import javax.annotation.Resource

import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator

@Component
class OrchestratorTypeResolver {

    @Resource
    private val hostOrchestrators: Map<String, HostOrchestrator>? = null

    @Resource
    private val containerOrchestrators: Map<String, ContainerOrchestrator>? = null

    @Throws(CloudbreakException::class)
    fun resolveType(name: String): OrchestratorType {
        if (hostOrchestrators!!.keys.contains(name)) {
            return OrchestratorType.HOST
        } else if (containerOrchestrators!!.keys.contains(name)) {
            return OrchestratorType.CONTAINER
        } else {
            LOGGER.error("Orchestrator not found: {}", name)
            throw CloudbreakException("Orchestrator not found: " + name)
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(OrchestratorTypeResolver::class.java)
    }


}
