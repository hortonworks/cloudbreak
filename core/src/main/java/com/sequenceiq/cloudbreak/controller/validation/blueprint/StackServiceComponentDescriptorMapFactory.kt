package com.sequenceiq.cloudbreak.controller.validation.blueprint

import org.springframework.beans.factory.FactoryBean

import com.fasterxml.jackson.databind.JsonNode
import com.google.api.client.util.Maps
import com.sequenceiq.cloudbreak.util.JsonUtil

class StackServiceComponentDescriptorMapFactory(private val stackServiceComponentsJson: String, private val minCardinalityReps: Map<String, Int>,
                                                private val maxCardinalityReps: Map<String, Int>) : FactoryBean<StackServiceComponentDescriptors> {

    @Throws(Exception::class)
    override fun getObject(): StackServiceComponentDescriptors {
        val stackServiceComponentDescriptorMap = Maps.newHashMap<String, StackServiceComponentDescriptor>()
        val rootNode = JsonUtil.readTree(stackServiceComponentsJson)
        val itemsNode = rootNode.get("items")
        for (itemNode in itemsNode) {
            val componentsNode = itemNode.get("components")
            for (componentNode in componentsNode) {
                val stackServiceComponentsNode = componentNode.get("StackServiceComponents")
                val componentDesc = createComponentDesc(stackServiceComponentsNode)
                stackServiceComponentDescriptorMap.put(componentDesc.name, componentDesc)
            }
        }
        return StackServiceComponentDescriptors(stackServiceComponentDescriptorMap)
    }

    override fun getObjectType(): Class<*> {
        return Map<Any, Any>::class.java
    }

    override fun isSingleton(): Boolean {
        return true
    }

    private fun createComponentDesc(stackServiceComponentNode: JsonNode): StackServiceComponentDescriptor {
        val componentName = stackServiceComponentNode.get("component_name").asText()
        val componentCategory = stackServiceComponentNode.get("component_category").asText()
        val minCardinality = parseMinCardinality(stackServiceComponentNode.get("cardinality").asText())
        val maxCardinality = parseMaxCardinality(stackServiceComponentNode.get("cardinality").asText())
        return StackServiceComponentDescriptor(componentName, componentCategory, minCardinality, maxCardinality)
    }

    private fun parseMinCardinality(cardinalityString: String): Int {
        val minCardinality = minCardinalityReps[cardinalityString]
        return minCardinality ?: 0
    }

    private fun parseMaxCardinality(cardinalityString: String): Int {
        val maxCardinality = maxCardinalityReps[cardinalityString]
        return maxCardinality ?: Integer.MAX_VALUE
    }
}
