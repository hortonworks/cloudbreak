package com.sequenceiq.it.spark.ambari

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.sequenceiq.it.spark.ITResponse

import spark.Request
import spark.Response

class AmbariBlueprintsResponse : ITResponse() {
    @Throws(Exception::class)
    override fun handle(request: Request, response: Response): Any {
        response.type("text/plain")
        val rootNode = JsonNodeFactory.instance.objectNode()
        rootNode.putObject("host_groups").putObject("components").putArray("name")
        return rootNode.toString()
    }
}
