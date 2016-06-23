package com.sequenceiq.cloudbreak.cloud.openstack.metadata

import org.openstack4j.api.OSClient
import org.openstack4j.model.compute.Server

import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData

interface CloudInstanceMetaDataExtractor {
    fun extractMetadata(client: OSClient, server: Server, instanceId: String): CloudInstanceMetaData
}

