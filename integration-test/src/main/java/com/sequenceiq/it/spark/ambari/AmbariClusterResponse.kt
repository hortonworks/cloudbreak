package com.sequenceiq.it.spark.ambari

import java.util.ArrayList

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.sequenceiq.it.spark.ambari.model.Clusters
import com.sequenceiq.it.spark.ambari.model.Hosts
import com.sequenceiq.it.spark.ITResponse

import spark.Request
import spark.Response

class AmbariClusterResponse(private val serverNumber: Int) : ITResponse() {

    @Throws(Exception::class)
    override fun handle(request: Request, response: Response): Any {
        response.type("text/plain")
        val rootNode = JsonNodeFactory.instance.objectNode()

        val ambariServers = ArrayList<String>()
        for (i in 1..sizeOfAmbariServers(serverNumber)) {
            ambariServers.add("127.0.0." + i)
        }

        rootNode.putObject("hosts").set("Hosts", objectMapper.valueToTree<JsonNode>(Hosts(ambariServers, "HEALTHY")))

        val items = rootNode.putArray("items")
        items.addObject().set("Clusters", objectMapper.valueToTree<JsonNode>(Clusters("ambari_cluster")))
        return rootNode
    }

    private fun sizeOfAmbariServers(serverNumber: Int): Int {
        return serverNumber - 1
    }
}
