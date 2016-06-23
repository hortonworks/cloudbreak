package com.sequenceiq.cloudbreak.controller.validation.blueprint

import java.io.IOException
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.stream.Collectors

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.Maps
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.InstanceGroup

@Component
class BlueprintValidator {

    private val objectMapper = ObjectMapper()

    @Inject
    private val stackServiceComponentDescs: StackServiceComponentDescriptors? = null

    fun validateBlueprintForStack(blueprint: Blueprint, hostGroups: Set<HostGroup>, instanceGroups: Set<InstanceGroup>) {
        try {
            val hostGroupsNode = getHostGroupNode(blueprint)
            validateHostGroups(hostGroupsNode, hostGroups, instanceGroups)
            val hostGroupMap = createHostGroupMap(hostGroups)
            val blueprintServiceComponentMap = Maps.newHashMap<String, BlueprintServiceComponent>()
            for (hostGroupNode in hostGroupsNode) {
                validateHostGroup(hostGroupNode, hostGroupMap, blueprintServiceComponentMap)
            }
            validateBlueprintServiceComponents(blueprintServiceComponentMap)
        } catch (e: IOException) {
            throw BadRequestException(String.format("Blueprint [%s] can not be parsed from JSON.", blueprint.id))
        }

    }

    @Throws(IOException::class)
    fun getHostGroupNode(blueprint: Blueprint): JsonNode {
        val blueprintJsonTree = createJsonTree(blueprint)
        return blueprintJsonTree.get("host_groups")
    }

    @Throws(IOException::class)
    private fun createJsonTree(blueprint: Blueprint): JsonNode {
        return objectMapper.readTree(blueprint.blueprintText)
    }

    private fun validateHostGroups(hostGroupsNode: JsonNode, hostGroups: Set<HostGroup>, instanceGroups: Set<InstanceGroup>) {
        val hostGroupsInRequest = getHostGroupsFromRequest(hostGroups)
        val hostGroupsInBlueprint = getHostGroupsFromBlueprint(hostGroupsNode)

        if (!hostGroupsInRequest.containsAll(hostGroupsInBlueprint) || !hostGroupsInBlueprint.containsAll(hostGroupsInRequest)) {
            throw BadRequestException(String.format("The host groups in the blueprint must match the hostgroups in the request."))
        }

        if (!instanceGroups.isEmpty()) {
            val instanceGroupNames = HashSet<String>()
            for (hostGroup in hostGroups) {
                val instanceGroupName = hostGroup.constraint.instanceGroup.groupName
                if (instanceGroupNames.contains(instanceGroupName)) {
                    throw BadRequestException(String.format(
                            "Instance group '%s' is assigned to more than one hostgroup.", instanceGroupName))
                }
                instanceGroupNames.add(instanceGroupName)
            }
            if (instanceGroups.size < hostGroupsInRequest.size) {
                throw BadRequestException("Each host group must have an instance group")
            }
        }
    }

    fun validateHostGroupScalingRequest(blueprint: Blueprint, hostGroup: HostGroup, adjustment: Int?) {
        try {
            val hostGroupsNode = getHostGroupNode(blueprint)
            val hostGroupMap = createHostGroupMap(setOf<HostGroup>(hostGroup))
            for (hostGroupNode in hostGroupsNode) {
                if (hostGroup.name == hostGroupNode.get("name").asText()) {
                    hostGroup.constraint.hostCount = hostGroup.constraint.hostCount!! + adjustment!!
                    try {
                        validateHostGroup(hostGroupNode, hostGroupMap, HashMap<String, BlueprintServiceComponent>())
                    } catch (be: BadRequestException) {
                        throw be
                    } finally {
                        hostGroup.constraint.hostCount = hostGroup.constraint.hostCount!! - adjustment
                    }
                    break
                }
            }
        } catch (e: IOException) {
            throw BadRequestException(String.format("Blueprint [%s] can not be parsed from JSON.", blueprint.id))
        }

    }

    private fun getHostGroupsFromRequest(hostGroup: Set<HostGroup>): Set<String> {
        return hostGroup.stream().map(Function<HostGroup, String> { it.getName() }).collect(Collectors.toSet<String>())
    }

    private fun getHostGroupsFromBlueprint(hostGroupsNode: JsonNode): Set<String> {
        val hostGroupsInBlueprint = HashSet<String>()
        val hostGroups = hostGroupsNode.elements()
        while (hostGroups.hasNext()) {
            hostGroupsInBlueprint.add(hostGroups.next().get("name").asText())
        }
        return hostGroupsInBlueprint
    }

    fun createHostGroupMap(hostGroups: Set<HostGroup>): Map<String, HostGroup> {
        val groupMap = Maps.newHashMap<String, HostGroup>()
        for (hostGroup in hostGroups) {
            groupMap.put(hostGroup.name, hostGroup)
        }
        return groupMap
    }

    private fun validateHostGroup(hostGroupNode: JsonNode, hostGroupMap: Map<String, HostGroup>,
                                  blueprintServiceComponentMap: MutableMap<String, BlueprintServiceComponent>) {
        val hostGroupName = getHostGroupName(hostGroupNode)
        val hostGroup = getHostGroup(hostGroupMap, hostGroupName)
        val componentsNode = getComponentsNode(hostGroupNode)
        for (componentNode in componentsNode) {
            validateComponent(componentNode, hostGroup, blueprintServiceComponentMap)
        }
    }

    fun getHostGroupName(hostGroupNode: JsonNode): String {
        return hostGroupNode.get("name").asText()
    }

    fun getHostGroup(hostGroupMap: Map<String, HostGroup>, hostGroupName: String): HostGroup {
        return hostGroupMap[hostGroupName]
    }

    fun getComponentsNode(hostGroupNode: JsonNode): JsonNode {
        return hostGroupNode.get("components")
    }

    private fun validateComponent(componentNode: JsonNode, hostGroup: HostGroup, blueprintServiceComponentMap: MutableMap<String, BlueprintServiceComponent>) {
        val componentName = componentNode.get("name").asText()
        val componentDescriptor = stackServiceComponentDescs!!.get(componentName)
        if (componentDescriptor != null) {
            validateComponentCardinality(componentDescriptor, hostGroup)
            updateBlueprintServiceComponentMap(componentDescriptor, hostGroup, blueprintServiceComponentMap)
        }
    }

    private fun validateComponentCardinality(componentDescriptor: StackServiceComponentDescriptor, hostGroup: HostGroup) {
        val nodeCount = hostGroup.constraint.hostCount!!
        val minCardinality = componentDescriptor.minCardinality
        val maxCardinality = componentDescriptor.maxCardinality
        if (componentDescriptor.isMaster && !isNodeCountCorrect(nodeCount, minCardinality, maxCardinality)) {
            throw BadRequestException(String.format(
                    "The node count '%d' for hostgroup '%s' cannot be less than '%d' or more than '%d' because of '%s' component", nodeCount,
                    hostGroup.name, minCardinality, maxCardinality, componentDescriptor.name))
        }
    }

    private fun validateBlueprintServiceComponents(blueprintServiceComponentMap: Map<String, BlueprintServiceComponent>) {
        for (blueprintServiceComponent in blueprintServiceComponentMap.values) {
            val componentName = blueprintServiceComponent.name
            val nodeCount = blueprintServiceComponent.nodeCount
            val stackServiceComponentDescriptor = stackServiceComponentDescs!!.get(componentName)
            if (stackServiceComponentDescriptor != null) {
                val minCardinality = stackServiceComponentDescriptor.minCardinality
                val maxCardinality = stackServiceComponentDescriptor.maxCardinality
                if (!isNodeCountCorrect(nodeCount, minCardinality, maxCardinality)) {
                    throw BadRequestException(String.format("Incorrect number of '%s' components are in '%s' hostgroups: count: %d, min: %d max: %d",
                            componentName, blueprintServiceComponent.hostgroups.toString(), nodeCount, minCardinality, maxCardinality))
                }
            }
        }
    }

    private fun isNodeCountCorrect(nodeCount: Int, minCardinality: Int, maxCardinality: Int): Boolean {
        return minCardinality <= nodeCount && nodeCount <= maxCardinality
    }

    private fun updateBlueprintServiceComponentMap(componentDescriptor: StackServiceComponentDescriptor, hostGroup: HostGroup,
                                                   blueprintServiceComponentMap: MutableMap<String, BlueprintServiceComponent>) {
        val componentName = componentDescriptor.name
        var blueprintServiceComponent: BlueprintServiceComponent? = blueprintServiceComponentMap[componentName]
        if (blueprintServiceComponent == null) {
            blueprintServiceComponent = BlueprintServiceComponent(componentName, hostGroup.name, hostGroup.constraint.hostCount!!)
            blueprintServiceComponentMap.put(componentName, blueprintServiceComponent)
        } else {
            blueprintServiceComponent.update(hostGroup)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BlueprintValidator::class.java)
    }
}
