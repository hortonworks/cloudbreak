package com.sequenceiq.cloudbreak.cloud.openstack.metadata

import javax.inject.Inject

import org.openstack4j.api.OSClient
import org.openstack4j.model.compute.Server
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData

@Component("cloudInstanceMetadataExtractor")
class PortOrComputeApiMetadataExtractor : CloudInstanceMetaDataExtractor {

    @Inject
    private val portApiExtractor: PortApiExtractor? = null

    @Inject
    private val computeApiExtractor: ComputeApiExtractor? = null

    override fun extractMetadata(client: OSClient, server: Server, instanceId: String): CloudInstanceMetaData {
        if (server.addresses.addresses.isEmpty()) {
            return portApiExtractor!!.extractMetadata(client, server, instanceId)
        } else {
            return computeApiExtractor!!.extractMetadata(client, server, instanceId)
        }
    }
}
