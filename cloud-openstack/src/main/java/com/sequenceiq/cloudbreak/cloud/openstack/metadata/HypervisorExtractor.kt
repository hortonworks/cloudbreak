package com.sequenceiq.cloudbreak.cloud.openstack.metadata

import org.openstack4j.model.compute.Server
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HypervisorExtractor {

    fun getHypervisor(server: Server): String {
        LOGGER.info("Hypervisor info for instance: {}. HypervisorHostname: {}, Host: {}", server.instanceName, server.hypervisorHostname, server.host)
        var hypervisor: String? = server.hypervisorHostname
        LOGGER.info("Hypervisor for instance: {} is: {}", server.instanceName, server.hypervisorHostname, server.host)
        if (hypervisor == null) {
            hypervisor = server.host
        }
        LOGGER.info("Used hypervisor for instance: {}. hypervisor: {}", server.instanceName, hypervisor)
        return hypervisor
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(HypervisorExtractor::class.java)
    }
}
