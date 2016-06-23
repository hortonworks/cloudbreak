package com.sequenceiq.cloudbreak.cloud.arm

import org.apache.commons.lang3.StringUtils.isNoneEmpty

import java.util.ArrayList

import org.apache.commons.lang3.text.WordUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.google.common.base.Splitter
import com.sequenceiq.cloud.azure.client.AzureRMClient
import com.sequenceiq.cloudbreak.cloud.arm.status.ArmStackStatus
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus
import com.sequenceiq.cloudbreak.common.type.ResourceType

import groovyx.net.http.HttpResponseException

@Component
class ArmUtils {

    @Value("${cb.max.azure.resource.name.length:}")
    private val maxResourceNameLength: Int = 0


    fun getTemplateResource(resourceList: List<CloudResource>): CloudResource {
        for (resource in resourceList) {
            if (resource.type == ResourceType.ARM_TEMPLATE) {
                return resource
            }
        }
        throw CloudConnectorException(String.format("No resource found: %s", ResourceType.ARM_TEMPLATE))
    }

    fun getPrivateInstanceId(stackName: String, groupName: String, privateId: String): String {
        return String.format("%s%s%s", stackName, getGroupName(groupName), privateId)
    }

    fun getStackName(cloudContext: CloudContext): String {
        return String(Splitter.fixedLength(maxResourceNameLength - cloudContext.id!!.toString().length).splitToList(cloudContext.name)[0] + cloudContext.id!!)
    }

    fun getLoadBalancerId(stackName: String): String {
        return String.format("%s%s", stackName, "lb")
    }

    fun templateStatus(resource: CloudResource, templateDeployment: Map<String, Any>, access: AzureRMClient, stackName: String): CloudResourceStatus {
        val status = (templateDeployment["properties"] as Map<Any, Any>)["provisioningState"].toString()
        LOGGER.info("Arm stack status of: {}  is: {}", resource.name, status)
        val resourceStatus = ArmStackStatus.mapResourceStatus(status)
        var armResourceStatus: CloudResourceStatus? = null
        if (ResourceStatus.FAILED == resourceStatus) {
            LOGGER.debug("Cloud resource status: {}", resourceStatus)
            try {
                val templateDeploymentOperations = access.getTemplateDeploymentOperations(stackName, stackName)
                val value = templateDeploymentOperations["value"] as ArrayList<Map<Any, Any>>
                for (map in value) {
                    val properties = map["properties"] as Map<Any, Any>
                    if ("Failed" == properties["provisioningState"].toString()) {
                        val statusMessage = properties["statusMessage"] as Map<Any, Any>
                        val error = statusMessage["error"] as Map<Any, Any>
                        val message = error["message"].toString()
                        armResourceStatus = CloudResourceStatus(resource, ArmStackStatus.mapResourceStatus(status), message)
                        break
                    }
                }

            } catch (e: HttpResponseException) {
                armResourceStatus = CloudResourceStatus(resource, ArmStackStatus.mapResourceStatus(status), e.response.data.toString())
            } catch (e: Exception) {
                armResourceStatus = CloudResourceStatus(resource, ArmStackStatus.mapResourceStatus(status), e.message)
            }

        } else {
            LOGGER.debug("Cloud resource status: {}", resourceStatus)
            armResourceStatus = CloudResourceStatus(resource, ArmStackStatus.mapResourceStatus(status))
        }
        return armResourceStatus
    }


    fun getResourceGroupName(cloudContext: CloudContext): String {
        return getStackName(cloudContext)
    }


    fun isExistingNetwork(network: Network): Boolean {
        return isNoneEmpty(getCustomNetworkId(network)) && isNoneEmpty(getCustomResourceGroupName(network)) && isNoneEmpty(getCustomSubnetId(network))
    }

    fun getCustomNetworkId(network: Network): String {
        return network.getStringParameter(NETWORK_ID)
    }

    fun getCustomResourceGroupName(network: Network): String {
        return network.getStringParameter(RG_NAME)
    }

    fun getCustomSubnetId(network: Network): String {
        return network.getStringParameter(SUBNET_ID)
    }

    fun validateSubnetRules(client: AzureRMClient, network: Network) {
        if (isExistingNetwork(network)) {
            val resourceGroupName = getCustomResourceGroupName(network)
            val networkId = getCustomNetworkId(network)
            val subnetId = getCustomSubnetId(network)
            val subnetProperties = client.getSubnetProperties(resourceGroupName, networkId, subnetId)
            val networkSecurityGroup = subnetProperties["networkSecurityGroup"] as Map<Any, Any>
            if (networkSecurityGroup != null) {
                validateSecurityGroup(client, networkSecurityGroup)
            }
        }
    }

    fun validateStorageType(stack: CloudStack) {
        for (group in stack.groups) {
            val template = group.instances[0].template
            val flavor = template.flavor
            val volumeType = template.volumeType
            val diskType = ArmDiskType.getByValue(volumeType)
            if (ArmDiskType.PREMIUM_LOCALLY_REDUNDANT == diskType && !flavor.contains("_DS")) {
                throw CloudConnectorException("Only the DS instance types supports the premium storage.")
            }
        }
    }

    private fun validateSecurityGroup(client: AzureRMClient, networkSecurityGroup: Map<Any, Any>) {
        val securityGroupId = networkSecurityGroup["id"] as String
        val parts = securityGroupId.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        if (parts.size != ID_SEGMENTS) {
            LOGGER.info("Cannot get the security group's properties, id: {}", securityGroupId)
            return
        }
        val securityGroupProperties = client.getSecurityGroupProperties(parts[RG_PART], parts[SEC_GROUP_PART])
        LOGGER.info("Retrieved security group properties: {}", securityGroupProperties)
        val securityRules = securityGroupProperties["securityRules"] as List<Any>
        var port22Found = false
        var port443Found = false
        for (securityRule in securityRules) {
            val rule = securityRule as Map<Any, Any>
            val properties = rule["properties"] as Map<Any, Any>
            if (isValidInboundRule(properties)) {
                val destinationPortRange = properties["destinationPortRange"] as String
                if ("*" == destinationPortRange) {
                    return
                }
                val range = destinationPortRange.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                port443Found = if (port443Found) port443Found else isPortFound(PORT_443, range)
                port22Found = if (port22Found) port22Found else isPortFound(PORT_22, range)
                if (port22Found && port443Found) {
                    return
                }
            }
        }
        throw CloudConnectorException("The specified subnet's security group does not allow traffic for port 22 and/or 443")
    }

    private fun isValidInboundRule(properties: Map<Any, Any>): Boolean {
        val protocol = properties["protocol"].toString().toLowerCase()
        return "inbound" == properties["direction"].toString().toLowerCase()
                && ("tcp" == protocol || "*" == protocol)
                && "allow" == properties["access"].toString().toLowerCase()
    }

    private fun isPortFound(port: Int, destinationPortRange: Array<String>): Boolean {
        if (destinationPortRange.size == PORT_RANGE_NUM) {
            return isPortInRange(port, destinationPortRange)
        }
        return isPortMatch(port, destinationPortRange[0])
    }

    private fun isPortInRange(port: Int, range: Array<String>): Boolean {
        return Integer.parseInt(range[0]) <= port && Integer.parseInt(range[1]) >= port
    }

    private fun isPortMatch(port: Int, destinationPortRange: String): Boolean {
        return port == Integer.parseInt(destinationPortRange)
    }

    companion object {

        val NOT_FOUND = 404

        private val LOGGER = LoggerFactory.getLogger(ArmUtils::class.java)
        private val RG_NAME = "resourceGroupName"
        private val SUBNET_ID = "subnetId"
        private val NETWORK_ID = "networkId"
        private val PORT_22 = 22
        private val PORT_443 = 443
        private val PORT_RANGE_NUM = 2
        private val RG_PART = 4
        private val ID_SEGMENTS = 9
        private val SEC_GROUP_PART = 8
        private val HOST_GROUP_LENGTH = 3

        fun getGroupName(group: String): String {
            val shortened = WordUtils.initials(group.replace("_".toRegex(), " ")).toLowerCase()
            return if (shortened.length <= HOST_GROUP_LENGTH) shortened else shortened.substring(0, HOST_GROUP_LENGTH)
        }
    }

}
