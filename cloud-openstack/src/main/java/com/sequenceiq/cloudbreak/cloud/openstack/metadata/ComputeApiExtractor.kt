package com.sequenceiq.cloudbreak.cloud.openstack.metadata

import javax.inject.Inject

import org.openstack4j.api.OSClient
import org.openstack4j.model.compute.Address
import org.openstack4j.model.compute.Server
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData

@Component
class ComputeApiExtractor : CloudInstanceMetaDataExtractor {

    @Inject
    private val hypervisorExtractor: HypervisorExtractor? = null

    override fun extractMetadata(client: OSClient, server: Server, instanceId: String): CloudInstanceMetaData {
        val hypervisor = hypervisorExtractor!!.getHypervisor(server)
        var privateIp: String? = null
        var floatingIp: String? = null
        val adrMap = server.addresses.addresses
        LOGGER.debug("Address map: {} of instance: {}", adrMap, server.name)
        for (key in adrMap.keys) {
            LOGGER.debug("Network resource key: {} of instance: {}", key, server.name)
            val adrList = adrMap[key]
            for (adr in adrList) {
                LOGGER.debug("Network resource key: {} of instance: {}, address: {}", key, server.name, adr)
                when (adr.getType()) {
                    "fixed" -> {
                        privateIp = adr.getAddr()
                        LOGGER.info("PrivateIp of instance: {} is {}", server.name, privateIp)
                    }
                    "floating" -> {
                        floatingIp = adr.getAddr()
                        LOGGER.info("FloatingIp of instance: {} is {}", server.name, floatingIp)
                    }
                    else -> LOGGER.error("No such network resource type: {}, instance: {}", adr.getType(), server.name)
                }
            }
        }
        return CloudInstanceMetaData(privateIp, floatingIp, hypervisor)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ComputeApiExtractor::class.java)
    }
}
