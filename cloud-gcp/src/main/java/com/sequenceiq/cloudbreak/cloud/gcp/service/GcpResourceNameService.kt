package com.sequenceiq.cloudbreak.cloud.gcp.service

import java.util.Date

import org.apache.commons.lang3.text.WordUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service("GcpResourceNameServiceV2")
class GcpResourceNameService : CloudbreakResourceNameService() {

    @Value("${cb.max.gcp.resource.name.length:}")
    private val maxResourceNameLength: Int = 0

    override fun resourceName(resourceType: ResourceType, vararg parts: Any): String {
        val resourceName: String

        when (resourceType) {
            ResourceType.GCP_NETWORK -> resourceName = gcpNetworkResourceName(parts)
            ResourceType.GCP_SUBNET -> resourceName = gcpSubnetResourceName(parts)
            ResourceType.GCP_FIREWALL_INTERNAL -> resourceName = stackBasedResourceWithSuffix(FIREWALL_INTERNAL_NAME_SUFFIX, parts)
            ResourceType.GCP_FIREWALL_IN -> resourceName = stackBasedResourceWithSuffix(FIREWALL_IN_NAME_SUFFIX, parts)
            ResourceType.GCP_RESERVED_IP -> resourceName = stackBasedResourceWithSuffix(RESERVED_IP_SUFFIX, parts)
            ResourceType.GCP_INSTANCE -> resourceName = instanceName(parts)
            ResourceType.GCP_DISK -> resourceName = instanceName(parts)
            ResourceType.GCP_ATTACHED_DISK -> resourceName = attachedDiskResourceName(parts)
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
        val instanceGroupName = WordUtils.initials(parts[1].toString().replace("_".toRegex(), " "))
        val privateId = parts[2].toString()

        name = normalize(stackName)
        name = adjustPartLength(name)
        name = appendPart(name, normalize(instanceGroupName))
        name = appendPart(name, privateId)
        name = appendHash(name, Date())
        name = adjustBaseLength(name, maxResourceNameLength)

        return name
    }

    private fun stackBasedResourceWithSuffix(suffix: String, parts: Array<Any>): String {
        checkArgs(1, *parts)
        val stackName = parts[0].toString()
        LOGGER.debug("Generating stack based resource name with suffix. Stack {}; suffix {}", parts, suffix)
        var name = normalize(stackName)
        name = adjustPartLength(name)
        name = appendPart(name, suffix)
        name = appendHash(name, Date())
        name = adjustBaseLength(name, maxResourceNameLength)
        return name
    }

    private fun gcpNetworkResourceName(parts: Array<Any>): String {
        checkArgs(1, *parts)
        var networkName: String? = null
        val stackName = parts[0].toString()
        networkName = normalize(stackName)
        networkName = adjustPartLength(networkName)
        networkName = appendHash(networkName, Date())
        networkName = adjustBaseLength(networkName, maxResourceNameLength)
        return networkName
    }

    private fun gcpSubnetResourceName(parts: Array<Any>): String {
        checkArgs(1, *parts)
        var subnetName: String? = null
        val stackName = parts[0].toString()
        subnetName = normalize(stackName)
        subnetName = adjustPartLength(subnetName)
        subnetName = appendHash(subnetName, Date())
        subnetName = adjustBaseLength(subnetName, maxResourceNameLength)
        return subnetName
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(GcpResourceNameService::class.java)

        private val FIREWALL_INTERNAL_NAME_SUFFIX = "internal"
        private val FIREWALL_IN_NAME_SUFFIX = "in"
        private val RESERVED_IP_SUFFIX = "reserved-ip"
        private val ATTACHED_DISKS_PART_COUNT = 4
        private val INSTANCE_NAME_PART_COUNT = 3
    }
}
