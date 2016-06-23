package com.sequenceiq.it.spark.ambari

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.sequenceiq.it.spark.ambari.model.Requests
import com.sequenceiq.it.spark.ITResponse

import spark.Request
import spark.Response

class AmbariClusterRequestsResponse : ITResponse() {
    @Throws(Exception::class)
    override fun handle(request: Request, response: Response): Any {
        response.type("text/plain")
        val rootNode = JsonNodeFactory.instance.objectNode()
        val requests = Requests(66, "SUCCESSFUL", 100)
        rootNode.set("Requests", ObjectMapper().valueToTree<JsonNode>(requests))
        return rootNode.toString()
    }
}
