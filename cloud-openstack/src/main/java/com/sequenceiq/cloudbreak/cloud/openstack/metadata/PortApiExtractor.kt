package com.sequenceiq.cloudbreak.cloud.openstack.metadata

import javax.inject.Inject

import org.openstack4j.api.OSClient
import org.openstack4j.model.compute.Server
import org.openstack4j.model.network.NetFloatingIP
import org.openstack4j.model.network.Port
import org.openstack4j.model.network.options.PortListOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.google.common.collect.Maps
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData

@Component
class PortApiExtractor : CloudInstanceMetaDataExtractor {

    @Inject
    private val hypervisorExtractor: HypervisorExtractor? = null

    override fun extractMetadata(client: OSClient, server: Server, instanceId: String): CloudInstanceMetaData {
        val hypervisor = hypervisorExtractor!!.getHypervisor(server)
        LOGGER.debug("Address map was empty, trying to extract ips")
        val ports = client.networking().port().list(getPortListOptions(instanceId))
        val portId = ports[0].id
        val floatingIps = client.networking().floatingip().list(getFloatingIpListOptions(portId))
        val ips = floatingIps[0]
        LOGGER.info("PrivateIp of instance: {} is {}", server.name, ips.fixedIpAddress)
        LOGGER.info("FloatingIp of instance: {} is {}", server.name, ips.floatingIpAddress)
        return CloudInstanceMetaData(ips.fixedIpAddress, ips.floatingIpAddress, hypervisor)
    }

    private fun getPortListOptions(instanceId: String): PortListOptions {
        return PortListOptions.create().deviceId(instanceId)
    }

    private fun getFloatingIpListOptions(portId: String): Map<String, String> {
        val paramMap = Maps.newHashMap<String, String>()
        paramMap.put("port_id", portId)
        return paramMap
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(PortApiExtractor::class.java)
    }
}
