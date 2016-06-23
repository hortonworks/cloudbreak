package com.sequenceiq.cloudbreak.cloud.openstack.heat

import java.util.ArrayList

import javax.inject.Inject
import javax.inject.Named

import org.openstack4j.api.OSClient
import org.openstack4j.model.compute.Server
import org.openstack4j.model.heat.Stack
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils

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

@Component
class OpenStackMetadataCollector : MetadataCollector {
    @Inject
    private val openStackClient: OpenStackClient? = null

    @Inject
    private val utils: OpenStackUtils? = null

    @Inject
    @Named("cloudInstanceMetadataExtractor")
    private val cloudInstanceMetaDataExtractor: CloudInstanceMetaDataExtractor? = null

    override fun collect(authenticatedContext: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmMetaDataStatus> {
        val resource = utils!!.getHeatResource(resources)

        val stackName = authenticatedContext.cloudContext.name
        val heatStackId = resource.name

        val templates = Lists.transform(vms) { input -> input!!.template }

        val templateMap = Maps.uniqueIndex(templates) { from -> utils.getPrivateInstanceId(from!!.groupName, java.lang.Long.toString(from.privateId!!)) }

        val client = openStackClient!!.createOSClient(authenticatedContext)

        val heatStack = client.heat().stacks().getDetails(stackName, heatStackId)

        val results = ArrayList<CloudVmMetaDataStatus>()


        val outputs = heatStack.outputs
        for (map in outputs) {
            val instanceUUID = map["output_value"] as String
            if (!StringUtils.isEmpty(instanceUUID)) {
                val server = client.compute().servers().get(instanceUUID)
                val metadata = server.metadata
                val privateInstanceId = utils.getPrivateInstanceId(metadata)
                val template = templateMap[privateInstanceId]
                if (template != null) {
                    val md = cloudInstanceMetaDataExtractor!!.extractMetadata(client, server, instanceUUID)
                    val cloudInstance = CloudInstance(instanceUUID, template)
                    val status = CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED)
                    results.add(CloudVmMetaDataStatus(status, md))
                }
            }
        }

        return results
    }
}
