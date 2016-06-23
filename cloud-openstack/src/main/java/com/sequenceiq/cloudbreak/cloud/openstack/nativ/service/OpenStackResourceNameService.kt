package com.sequenceiq.cloudbreak.cloud.openstack.nativ.service

import java.util.Date

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service("OpenStackResourceNameService")
class OpenStackResourceNameService : CloudbreakResourceNameService() {

    @Value("${cb.max.openstack.resource.name.length:}")
    private val maxResourceNameLength: Int = 0

    override fun resourceName(resourceType: ResourceType, vararg parts: Any): String {
        val resourceName: String

        when (resourceType) {
            ResourceType.OPENSTACK_NETWORK -> resourceName = openStackNetworkResourceName(parts)
            ResourceType.OPENSTACK_SUBNET -> resourceName = openStackNetworkResourceName(parts)
            ResourceType.OPENSTACK_ROUTER -> resourceName = openStackNetworkResourceName(parts)
            ResourceType.OPENSTACK_SECURITY_GROUP -> resourceName = openStackNetworkResourceName(parts)
            ResourceType.OPENSTACK_INSTANCE, ResourceType.OPENSTACK_PORT, ResourceType.OPENSTACK_FLOATING_IP -> resourceName = instanceName(parts)
            ResourceType.OPENSTACK_ATTACHED_DISK -> resourceName = attachedDiskResourceName(parts)
            else -> throw IllegalStateException("Unsupported resource type: " + resourceType)
        }
        return resourceName
    }

    private fun attachedDiskResourceName(parts: Array<Any>): String {
        checkArgs(ATTACHED_DISKS_PART_COUNT, *parts)
        val cnt = parts[ATTACHED_DISKS_PART_COUNT - 1].toString()
        var name = instanceName(parts)
        name = trimHash(name)
        name = appendPart(name, cnt)
        name = appendHash(name, Date())
        name = adjustBaseLength(name, maxResourceNameLength)
        return name
    }

    private fun instanceName(parts: Array<Any>): String {
        checkArgs(INSTANCE_NAME_PART_COUNT, *parts)
        var name: String? = null
        val stackName = parts[0].toString()
        val instanceGroupName = parts[1].toString()
        val privateId = parts[2].toString()

        name = normalize(stackName)
        name = adjustPartLength(name)
        name = appendPart(name, normalize(instanceGroupName))
        name = appendPart(name, privateId)
        name = appendHash(name, Date())
        name = adjustBaseLength(name, maxResourceNameLength)

        return name
    }

    private fun openStackNetworkResourceName(parts: Array<Any>): String {
        checkArgs(1, *parts)
        var networkName: String? = null
        val stackName = parts[0].toString()
        networkName = normalize(stackName)
        networkName = adjustPartLength(networkName)
        networkName = appendHash(networkName, Date())
        networkName = adjustBaseLength(networkName, maxResourceNameLength)
        return networkName
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(OpenStackResourceNameService::class.java)

        private val ATTACHED_DISKS_PART_COUNT = 4
        private val INSTANCE_NAME_PART_COUNT = 3
    }
}
