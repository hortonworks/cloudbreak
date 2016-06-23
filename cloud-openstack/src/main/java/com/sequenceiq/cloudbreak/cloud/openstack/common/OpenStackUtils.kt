package com.sequenceiq.cloudbreak.cloud.openstack.common

import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.NETWORK_ID
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.ROUTER_ID
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.SUBNET_ID
import org.apache.commons.lang3.StringUtils.isNoneEmpty

import javax.inject.Inject

import org.openstack4j.api.OSClient
import org.openstack4j.model.heat.Stack
import org.openstack4j.model.network.Subnet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.google.common.base.Splitter
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient
import com.sequenceiq.cloudbreak.cloud.openstack.status.HeatStackStatus
import com.sequenceiq.cloudbreak.cloud.openstack.view.NeutronNetworkView
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Component
class OpenStackUtils {

    @Value("${cb.max.openstack.resource.name.length:}")
    private val maxResourceNameLength: Int = 0

    @Inject
    private val openStackClient: OpenStackClient? = null

    fun getHeatResource(resourceList: List<CloudResource>): CloudResource {
        for (resource in resourceList) {
            if (resource.type === ResourceType.HEAT_STACK) {
                return resource
            }
        }
        throw CloudConnectorException(String.format("No resource found: %s", ResourceType.HEAT_STACK))
    }

    fun getPrivateInstanceId(groupName: String, privateId: String): String {
        return getNormalizedGroupName(groupName) + "_"
        +privateId
    }

    fun getPrivateInstanceId(metadata: Map<String, String>): String {
        return getPrivateInstanceId(metadata[CB_INSTANCE_GROUP_NAME], metadata[CB_INSTANCE_PRIVATE_ID])
    }

    fun getNormalizedGroupName(groupName: String): String {
        return groupName.replace("_".toRegex(), "")
    }

    fun heatStatus(resource: CloudResource, heatStack: Stack): CloudResourceStatus {
        val status = heatStack.status
        LOGGER.info("Heat stack status of: {}  is: {}", heatStack, status)
        val heatResourceStatus = CloudResourceStatus(resource, HeatStackStatus.mapResourceStatus(status), heatStack.stackStatusReason)
        LOGGER.debug("Cloud resource status: {}", heatResourceStatus)
        return heatResourceStatus
    }

    fun adjustStackNameLength(stackName: String): String {
        return String(Splitter.fixedLength(maxResourceNameLength).splitToList(stackName)[0])
    }

    fun isExistingNetwork(network: Network): Boolean {
        return isNoneEmpty(getCustomNetworkId(network))
    }

    fun assignFloatingIp(network: Network): Boolean {
        return NeutronNetworkView(network).assignFloatingIp()
    }

    fun getCustomNetworkId(network: Network): String {
        return network.getStringParameter(NETWORK_ID)
    }

    fun getCustomRouterId(network: Network): String {
        return network.getStringParameter(ROUTER_ID)
    }

    fun isExistingSubnet(network: Network): Boolean {
        return isNoneEmpty(getCustomSubnetId(network))
    }

    fun getCustomSubnetId(network: Network): String {
        return network.getStringParameter(SUBNET_ID)
    }

    fun getExistingSubnetCidr(authenticatedContext: AuthenticatedContext, network: Network): String? {
        if (isExistingSubnet(network)) {
            val subnetId = getCustomSubnetId(network)
            val osClient = openStackClient!!.createOSClient(authenticatedContext)
            val subnet = osClient.networking().subnet().get(subnetId) ?: throw CloudConnectorException("The specified subnet does not exist: " + subnetId)
            return subnet.cidr
        }
        return null
    }

    companion object {

        val CB_INSTANCE_GROUP_NAME = "cb_instance_group_name"
        val CB_INSTANCE_PRIVATE_ID = "cb_instance_private_id"
        private val LOGGER = LoggerFactory.getLogger(OpenStackUtils::class.java)
    }

}
