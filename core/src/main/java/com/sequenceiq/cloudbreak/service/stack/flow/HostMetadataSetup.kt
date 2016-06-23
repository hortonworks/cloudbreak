package com.sequenceiq.cloudbreak.service.stack.flow

import java.util.Collections.singletonMap

import java.util.Collections
import java.util.stream.Collectors

import javax.inject.Inject
import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils

import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.stack.StackService
import com.sequenceiq.cloudbreak.client.RestClientUtil

@Service
class HostMetadataSetup {

    @Value("${cb.host.discovery.custom.domain:}")
    private val customDomain: String? = null

    @Inject
    private val stackService: StackService? = null

    @Inject
    private val instanceMetaDataRepository: InstanceMetaDataRepository? = null

    @Inject
    private val tlsSecurityService: TlsSecurityService? = null

    @Throws(CloudbreakSecuritySetupException::class)
    fun setupHostMetadata(stackId: Long?) {
        LOGGER.info("Setting up host metadata for the cluster.")
        val stack = stackService!!.getById(stackId)
        val allInstanceMetaData = stack.runningInstanceMetaData
        val gateway = stack.gatewayInstanceGroup
        val gatewayInstance = gateway.instanceMetaData.iterator().next()
        val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stackId, gatewayInstance.publicIpWrapper)
        updateWithHostData(clientConfig, stack, emptySet<InstanceMetaData>())
        instanceMetaDataRepository!!.save(allInstanceMetaData)
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun setupNewHostMetadata(stackId: Long?, newAddresses: Set<String>) {
        LOGGER.info("Extending host metadata.")
        val stack = stackService!!.getById(stackId)
        val gateway = stack.gatewayInstanceGroup
        val gatewayInstance = gateway.instanceMetaData.iterator().next()
        val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stackId, gatewayInstance.publicIpWrapper)
        val newInstanceMetadata = stack.runningInstanceMetaData.stream().filter({ instanceMetaData -> newAddresses.contains(instanceMetaData.getPrivateIp()) }).collect(Collectors.toSet<InstanceMetaData>())
        updateWithHostData(clientConfig, stack, newInstanceMetadata)
        instanceMetaDataRepository!!.save<InstanceMetaData>(newInstanceMetadata)
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun updateWithHostData(clientConfig: HttpClientConfig, stack: Stack, newInstanceMetadata: Set<InstanceMetaData>?) {
        var restClient: Client? = null
        try {
            restClient = RestClientUtil.createClient(clientConfig.serverCert, clientConfig.clientCert, clientConfig.clientKey)
            val metadataToUpdate: Set<InstanceMetaData>
            if (newInstanceMetadata == null || newInstanceMetadata.isEmpty()) {
                metadataToUpdate = stack.runningInstanceMetaData
            } else {
                metadataToUpdate = newInstanceMetadata
            }
            val privateIps = metadataToUpdate.stream().map(Function<InstanceMetaData, String> { it.getPrivateIp() }).collect(Collectors.toList<String>())
            val target = RestClientUtil.createTarget(restClient, String.format("https://%s:%s", clientConfig.apiAddress, clientConfig.apiPort))
            val responses = target.path(HOSTNAME_ENDPOINT).request().post(Entity.json(singletonMap<String, List<String>>("clients", privateIps))).readEntity<GenericResponses>(GenericResponses::class.java)
            val members = responses.getResponses().stream().collect(Collectors.toMap<GenericResponse, String, String>(Function<GenericResponse, String> { it.getAddress() }, Function<GenericResponse, String> { it.getStatus() }))
            LOGGER.info("Received host names from hosts: {}, original targets: {}", members.keys, privateIps)
            for (instanceMetaData in metadataToUpdate) {
                val privateIp = instanceMetaData.privateIp
                val address = members.get(privateIp)
                // TODO remove column
                instanceMetaData.consulServer = false
                val fqdn = determineFqdn(instanceMetaData.instanceId, instanceMetaData.privateIp, address)
                instanceMetaData.discoveryFQDN = fqdn
                LOGGER.info("Domain used for isntance: {} original: {}, fqdn: {}", instanceMetaData.instanceId, address,
                        instanceMetaData.discoveryFQDN)
            }
        } catch (e: Exception) {
            throw CloudbreakSecuritySetupException(e)
        } finally {
            if (restClient != null) {
                restClient.close()
            }
        }
    }

    private fun determineFqdn(id: String, ip: String, address: String): String {
        val fqdn: String
        if (StringUtils.isEmpty(customDomain)) {
            if (address.split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray().size == 1) {
                //if there is no domain, we need to add one since ambari fails without domain, actually it does nothing just hangs...
                fqdn = address + DEFAULT_DOMAIN
                LOGGER.warn("Default domain is used, since there is no proper domain configured for instance: {}, ip: {}, original: {}, fqdn: {}",
                        id, ip, address, fqdn)
            } else {
                fqdn = address
            }
        } else {
            val hostname = address.split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0]
            // this is just for convenience
            if (customDomain!!.startsWith(".")) {
                fqdn = hostname + customDomain
            } else {
                fqdn = hostname + "." + customDomain
            }
            LOGGER.warn("Domain of instance will be overwritten instance: {}, ip: {}, original: {}, fqdn: {}",
                    id, ip, address, fqdn)
        }
        return fqdn
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(HostMetadataSetup::class.java)
        private val HOSTNAME_ENDPOINT = "saltboot/hostname/distribute"
        private val DEFAULT_DOMAIN = ".example.com"
    }

}