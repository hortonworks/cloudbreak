package com.sequenceiq.cloudbreak.cloud.openstack.nativ

import java.util.ArrayList

import javax.inject.Inject
import javax.inject.Named

import org.openstack4j.api.OSClient
import org.openstack4j.model.compute.Server
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.google.common.base.Function
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.sequenceiq.cloudbreak.cloud.MetadataCollector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils
import com.sequenceiq.cloudbreak.cloud.openstack.metadata.CloudInstanceMetaDataExtractor
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class OpenStackNativeMetaDataCollector : MetadataCollector {

    @Inject
    private val openStackClient: OpenStackClient? = null

    @Inject
    private val utils: OpenStackUtils? = null

    @Inject
    @Named("cloudInstanceMetadataExtractor")
    private val cloudInstanceMetaDataExtractor: CloudInstanceMetaDataExtractor? = null

    override fun collect(authenticatedContext: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmMetaDataStatus> {

        val templates = Lists.transform(vms) { input -> input!!.template }

        val templateMap = Maps.uniqueIndex(templates) { from -> utils!!.getPrivateInstanceId(from!!.groupName, java.lang.Long.toString(from.privateId!!)) }

        val client = openStackClient!!.createOSClient(authenticatedContext)
        val results = ArrayList<CloudVmMetaDataStatus>()

        for (resource in resources) {
            if (resource.type === ResourceType.OPENSTACK_INSTANCE) {
                val instanceUUID = resource.reference
                val server = client.compute().servers().get(instanceUUID)
                if (server != null) {
                    val metadata = server.metadata
                    val privateInstanceId = utils!!.getPrivateInstanceId(metadata)
                    val template = templateMap[privateInstanceId]
                    if (template != null) {
                        val md = cloudInstanceMetaDataExtractor!!.extractMetadata(client, server, instanceUUID)
                        val cloudInstance = CloudInstance(instanceUUID, template)
                        val status = CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED)
                        results.add(CloudVmMetaDataStatus(status, md))
                    }
                }
            }
        }
        return results
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(OpenStackNativeMetaDataCollector::class.java)
    }
}
