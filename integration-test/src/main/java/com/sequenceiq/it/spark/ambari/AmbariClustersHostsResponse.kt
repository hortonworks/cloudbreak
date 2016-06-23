package com.sequenceiq.it.spark.ambari

import java.util.Collections

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.sequenceiq.it.spark.ambari.model.Hosts
import com.sequenceiq.it.spark.ITResponse

import spark.Request
import spark.Response

class AmbariClustersHostsResponse(private val serverNumber: Int) : ITResponse() {

    @Throws(Exception::class)
    override fun handle(request: Request, response: Response): Any {
        response.type("text/plain")
        val rootNode = JsonNodeFactory.instance.objectNode()
        val items = rootNode.putArray("items")

        for (i in 1..serverNumber) {
            val hosts = Hosts(listOf<String>("host" + i), "HEALTHY")
            val item = items.addObject()
            item.set("Hosts", objectMapper.valueToTree<JsonNode>(hosts))

            item.putArray("host_components").addObject().putArray("HostRoles").addObject().put("component_name", "component-name").put("state", "SUCCESSFUL")
        }
        return rootNode
    }
}
