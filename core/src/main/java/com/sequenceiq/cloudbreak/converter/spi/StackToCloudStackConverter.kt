package com.sequenceiq.cloudbreak.converter.spi

import java.util.ArrayList
import java.util.Collections

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Security
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule
import com.sequenceiq.cloudbreak.cloud.model.Subnet
import com.sequenceiq.cloudbreak.cloud.model.Volume
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.domain.Template
import com.sequenceiq.cloudbreak.domain.json.Json
import com.sequenceiq.cloudbreak.repository.SecurityRuleRepository
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException
import com.sequenceiq.cloudbreak.service.image.ImageService
import com.sequenceiq.cloudbreak.service.stack.connector.VolumeUtils

@Component
class StackToCloudStackConverter {

    @Inject
    private val securityRuleRepository: SecurityRuleRepository? = null

    @Inject
    private val imageService: ImageService? = null

    fun convert(stack: Stack): CloudStack {
        return convert(stack, emptySet<String>())
    }

    fun convertForDownscale(stack: Stack, deleteRequestedInstances: Set<String>): CloudStack {
        return convert(stack, deleteRequestedInstances)
    }

    fun convertForTermination(stack: Stack, instanceId: String): CloudStack {
        return convert(stack, setOf<String>(instanceId))
    }

    private fun convert(stack: Stack, deleteRequestedInstances: Set<String>): CloudStack {
        try {
            val instanceGroups = buildInstanceGroups(stack.instanceGroupsAsList, deleteRequestedInstances)
            val image = imageService!!.getImage(stack.id)
            val network = buildNetwork(stack)
            val security = buildSecurity(stack)
            return CloudStack(instanceGroups, network, security, image, stack.parameters)
        } catch (inf: CloudbreakImageNotFoundException) {
            throw CloudbreakServiceException(inf)
        }

    }

    fun buildInstanceGroups(instanceGroups: List<InstanceGroup>, deleteRequests: Set<String>): List<Group> {
        // sort by name to avoid shuffling the different instance groups
        Collections.sort(instanceGroups)
        val groups = ArrayList<Group>()
        var privateId = getFirstValidPrivateId(instanceGroups)
        for (instanceGroup in instanceGroups) {
            val instances = ArrayList<CloudInstance>()
            val template = instanceGroup.template
            val desiredNodeCount = instanceGroup.nodeCount!!
            // existing instances
            for (metaData in instanceGroup.instanceMetaData) {
                val status = getInstanceStatus(metaData, deleteRequests)
                instances.add(buildInstance(metaData.instanceId, template, instanceGroup.groupName, metaData.privateId, status))
            }
            // new instances
            val existingNodesSize = instances.size
            if (existingNodesSize < desiredNodeCount) {
                for (i in 0..desiredNodeCount - existingNodesSize - 1) {
                    instances.add(buildInstance(null, template, instanceGroup.groupName, privateId++, InstanceStatus.CREATE_REQUESTED))
                }
            }
            groups.add(Group(instanceGroup.groupName, instanceGroup.instanceGroupType, instances))
        }
        return groups
    }

    fun buildInstances(stack: Stack): List<CloudInstance> {
        val groups = buildInstanceGroups(stack.instanceGroupsAsList, emptySet<String>())
        val cloudInstances = ArrayList<CloudInstance>()
        for (group in groups) {
            cloudInstances.addAll(group.instances)
        }
        return cloudInstances
    }

    fun buildInstance(id: String?, template: Template, name: String, privateId: Long?, status: InstanceStatus): CloudInstance {
        val instanceTemplate = buildInstanceTemplate(template, name, privateId, status)
        return CloudInstance(id, instanceTemplate)
    }

    fun buildInstanceTemplate(template: Template, name: String, privateId: Long?, status: InstanceStatus): InstanceTemplate {
        val attributes = template.attributes
        val fields = if (attributes == null) emptyMap<String, Any>() else attributes.map
        val volumes = ArrayList<Volume>()
        for (i in 0..template.volumeCount - 1) {
            val volume = Volume(VolumeUtils.VOLUME_PREFIX + (i + 1), template.volumeType, template.volumeSize!!)
            volumes.add(volume)
        }
        return InstanceTemplate(template.instanceType, name, privateId, volumes, status, fields)
    }

    private fun buildNetwork(stack: Stack): Network {
        val stackNetwork = stack.network
        val subnet = Subnet(stackNetwork.subnetCIDR)
        val attributes = stackNetwork.attributes
        val params = if (attributes == null) emptyMap<String, Any>() else attributes.map
        return Network(subnet, params)
    }

    private fun buildSecurity(stack: Stack): Security {
        val rules = ArrayList<SecurityRule>()
        if (stack.securityGroup == null) {
            return Security(rules)
        }
        val id = stack.securityGroup.id
        val securityRules = securityRuleRepository!!.findAllBySecurityGroupId(id)
        for (securityRule in securityRules) {
            rules.add(SecurityRule(securityRule.cidr, securityRule.ports, securityRule.protocol))
        }
        return Security(rules)
    }

    private fun getFirstValidPrivateId(instanceGroups: List<InstanceGroup>): Long {
        var highest: Long = 0
        for (instanceGroup in instanceGroups) {
            for (metaData in instanceGroup.instanceMetaData) {
                val privateId = metaData.privateId ?: continue
                if (privateId > highest) {
                    highest = privateId
                }
            }
        }
        return if (highest == 0) 0 else highest + 1
    }

    private fun getInstanceStatus(metaData: InstanceMetaData, deleteRequests: Set<String>): InstanceStatus {
        return if (deleteRequests.contains(metaData.instanceId))
            InstanceStatus.DELETE_REQUESTED
        else if (metaData.instanceStatus == com.sequenceiq.cloudbreak.api.model.InstanceStatus.REQUESTED)
            InstanceStatus.CREATE_REQUESTED
        else
            InstanceStatus.CREATED
    }

}
