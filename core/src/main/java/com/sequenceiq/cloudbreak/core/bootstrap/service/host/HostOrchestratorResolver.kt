package com.sequenceiq.cloudbreak.core.bootstrap.service.host

import javax.annotation.Resource

import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.CloudbreakException

@Component
class HostOrchestratorResolver {

    @Resource
    private val hostOrchestrators: Map<String, HostOrchestrator>? = null

    @Throws(CloudbreakException::class)
    operator fun get(name: String): HostOrchestrator {
        val co = hostOrchestrators!![name]
        if (co == null) {
            LOGGER.error("HostOrchestrator not found: {}, supported HostOrchestrator: {}", name, hostOrchestrators)
            throw CloudbreakException("HostOrchestrator not found: " + name)
        }
        return co
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(HostOrchestratorResolver::class.java)
    }
}
