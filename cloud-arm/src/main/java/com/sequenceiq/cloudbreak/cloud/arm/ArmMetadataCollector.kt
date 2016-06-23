package com.sequenceiq.cloudbreak.cloud.arm

import java.util.ArrayList

import javax.inject.Inject

import org.springframework.stereotype.Service

import com.google.common.base.Function
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.sequenceiq.cloud.azure.client.AzureRMClient
import com.sequenceiq.cloudbreak.cloud.MetadataCollector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate

import groovyx.net.http.HttpResponseException

@Service
class ArmMetadataCollector : MetadataCollector {

    @Inject
    private val armClient: ArmClient? = null

    @Inject
    private val armTemplateUtils: ArmUtils? = null

    override fun collect(authenticatedContext: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmMetaDataStatus> {
        val access = armClient!!.getClient(authenticatedContext.cloudCredential)
        val resource = armTemplateUtils!!.getTemplateResource(resources)
        val results = ArrayList<CloudVmMetaDataStatus>()

        val templates = Lists.transform(vms) { input -> input!!.template }

        val templateMap = Maps.uniqueIndex(templates) { from -> armTemplateUtils.getPrivateInstanceId(resource.name, from!!.groupName, java.lang.Long.toString(from.privateId!!)) }

        try {
            for (instance in templateMap.entries) {
                val network = (((access.getVirtualMachine(resource.name, instance.key) as Map<Any, Any>)["properties"] as Map<Any, Any>)["networkProfile"] as Map<Any, Any>)["networkInterfaces"] as ArrayList<Map<Any, Any>>
                val networkInterfaceName = getNameFromConnectionString(network[0]["id"].toString())
                val networkInterface = access.getNetworkInterface(resource.name, networkInterfaceName) as Map<Any, Any>
                val ips = (networkInterface["properties"] as Map<Any, Any>)["ipConfigurations"] as ArrayList<Any>
                val properties = (ips[0] as Map<Any, Any>)["properties"] as Map<Any, Any>
                var publicIp: String? = null
                if (properties["publicIPAddress"] == null) {
                    publicIp = access.getLoadBalancerIp(resource.name, armTemplateUtils.getLoadBalancerId(resource.name))
                } else {
                    val publicIPAddress = properties["publicIPAddress"] as Map<Any, Any>
                    val publicIpName = publicIPAddress["id"].toString()
                    val publicAdressObject = access.getPublicIpAddress(resource.name, getNameFromConnectionString(publicIpName)) as Map<Any, Any>
                    val publicIpProperties = publicAdressObject["properties"] as Map<Any, Any>
                    publicIp = publicIpProperties["ipAddress"].toString()
                }
                val privateIp = properties["privateIPAddress"].toString()
                val instanceId = instance.key
                if (publicIp == null) {
                    throw CloudConnectorException(String.format("Public ip address can not be null but it was on %s instance.", instance.key))
                }
                val md = CloudInstanceMetaData(privateIp, publicIp)

                val template = templateMap[instanceId]
                if (template != null) {
                    val cloudInstance = CloudInstance(instanceId, template)
                    val status = CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED)
                    results.add(CloudVmMetaDataStatus(status, md))

                }
            }

        } catch (e: HttpResponseException) {
            throw CloudConnectorException(e.response.data.toString(), e)
        } catch (e: Exception) {
            throw CloudConnectorException(e.message, e)
        }

        return results
    }

    private fun getNameFromConnectionString(connection: String): String {
        return connection.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[connection.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray().size - 1]
    }

}
