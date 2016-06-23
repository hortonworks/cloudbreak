package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint

import java.io.IOException
import java.util.HashSet

import org.springframework.stereotype.Component

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.sequenceiq.cloudbreak.util.JsonUtil

@Component
class JacksonBlueprintProcessor : BlueprintProcessor {

    override fun addConfigEntries(originalBlueprint: String, configurationEntries: List<BlueprintConfigurationEntry>, override: Boolean): String {
        try {
            val root = JsonUtil.readTree(originalBlueprint) as ObjectNode
            var configurationsNode = root.path(CONFIGURATIONS_NODE)
            if (configurationsNode.isMissingNode) {
                configurationsNode = root.putArray(CONFIGURATIONS_NODE)
            }
            val configurationsArrayNode = configurationsNode as ArrayNode
            for (configurationEntry in configurationEntries) {
                var configFileNode = configurationsArrayNode.findPath(configurationEntry.configFile)
                if (override || configFileNode.path("properties").findPath(configurationEntry.key).isMissingNode) {
                    if (configFileNode.isMissingNode) {
                        val arrayElementNode = configurationsArrayNode.addObject()
                        configFileNode = arrayElementNode.putObject(configurationEntry.configFile)
                    }
                    val propertiesNode = configFileNode.path("properties")
                    if (!propertiesNode.isMissingNode) {
                        (propertiesNode as ObjectNode).put(configurationEntry.key, configurationEntry.value)
                    } else {
                        (configFileNode as ObjectNode).put(configurationEntry.key, configurationEntry.value)
                    }
                }
            }
            return JsonUtil.writeValueAsString(root)
        } catch (e: IOException) {
            throw BlueprintProcessingException("Failed to add config entries to original blueprint.", e)
        }

    }

    override fun getComponentsInHostGroup(blueprintText: String, hostGroup: String): Set<String> {
        try {
            val services = HashSet<String>()
            val root = JsonUtil.readTree(blueprintText) as ObjectNode
            val hostGroupsNode = root.path(HOST_GROUPS_NODE) as ArrayNode
            val hostGroups = hostGroupsNode.elements()
            while (hostGroups.hasNext()) {
                val hostGroupNode = hostGroups.next()
                if (hostGroup == hostGroupNode.path("name").textValue()) {
                    val components = hostGroupNode.path("components").elements()
                    while (components.hasNext()) {
                        services.add(components.next().path("name").textValue())
                    }
                    break
                }
            }
            return services
        } catch (e: IOException) {
            throw BlueprintProcessingException("Failed to get components for hostgroup '$hostGroup' from blueprint.", e)
        }

    }

    override fun componentExistsInBlueprint(component: String, blueprintText: String): Boolean {
        var componentExists = false
        try {
            val root = JsonUtil.readTree(blueprintText) as ObjectNode
            val hostGroupsNode = root.path(HOST_GROUPS_NODE) as ArrayNode
            val hostGroups = hostGroupsNode.elements()
            while (hostGroups.hasNext() && !componentExists) {
                val hostGroupNode = hostGroups.next()
                componentExists = componentExistsInHostgroup(component, hostGroupNode)
            }
            return componentExists
        } catch (e: IOException) {
            throw BlueprintProcessingException("Failed to check that component('$component') exists in the blueprint.", e)
        }

    }

    override fun removeComponentFromBlueprint(component: String, blueprintText: String): String {
        try {
            val root = JsonUtil.readTree(blueprintText) as ObjectNode
            val hostGroupsNode = root.path(HOST_GROUPS_NODE) as ArrayNode
            val hostGroups = hostGroupsNode.elements()
            while (hostGroups.hasNext()) {
                val hostGroupNode = hostGroups.next()
                val components = hostGroupNode.path("components").elements()
                while (components.hasNext()) {
                    if (component == components.next().path("name").textValue()) {
                        components.remove()
                    }
                }
            }
            return JsonUtil.writeValueAsString(root)
        } catch (e: IOException) {
            throw BlueprintProcessingException("Failed to remove component('$component') from the blueprint.", e)
        }

    }

    override fun modifyHdpVersion(blueprintText: String, hdpVersion: String): String {
        try {
            val root = JsonUtil.readTree(blueprintText) as ObjectNode
            val blueprintsNode = root.path(BLUEPRINTS) as ObjectNode
            blueprintsNode.remove(STACK_VERSION)
            val split = hdpVersion.split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            blueprintsNode.put(STACK_VERSION, split[0] + "." + split[1])
            return JsonUtil.writeValueAsString(root)
        } catch (e: IOException) {
            throw BlueprintProcessingException("Failed to modify hdp version.", e)
        }

    }

    override fun addComponentToHostgroups(component: String, hostGroupNames: Collection<String>, blueprintText: String): String {
        try {
            val root = JsonUtil.readTree(blueprintText) as ObjectNode
            val hostGroupsNode = root.path(HOST_GROUPS_NODE) as ArrayNode
            val hostGroups = hostGroupsNode.elements()
            while (hostGroups.hasNext()) {
                val hostGroupNode = hostGroups.next()
                val hostGroupName = hostGroupNode.path("name").textValue()
                if (hostGroupNames.contains(hostGroupName) && !componentExistsInHostgroup(component, hostGroupNode)) {
                    val components = hostGroupNode.path("components") as ArrayNode
                    components.addPOJO(ComponentElement(component))
                }
            }
            return JsonUtil.writeValueAsString(root)
        } catch (e: IOException) {
            throw BlueprintProcessingException("Failed to remove component('$component') from the blueprint.", e)
        }

    }

    private fun componentExistsInHostgroup(component: String, hostGroupNode: JsonNode): Boolean {
        var componentExists = false
        val components = hostGroupNode.path("components").elements()
        while (components.hasNext()) {
            if (component == components.next().path("name").textValue()) {
                componentExists = true
                break
            }
        }
        return componentExists
    }

    private inner class ComponentElement private constructor(var name: String?)

    companion object {

        private val CONFIGURATIONS_NODE = "configurations"
        private val HOST_GROUPS_NODE = "host_groups"
        private val BLUEPRINTS = "Blueprints"
        private val STACK_VERSION = "stack_version"
    }
}
