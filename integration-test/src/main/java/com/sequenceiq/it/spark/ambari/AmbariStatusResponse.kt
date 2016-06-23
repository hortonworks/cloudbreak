package com.sequenceiq.it.spark.ambari

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.sequenceiq.it.spark.ambari.model.Requests
import com.sequenceiq.it.spark.ITResponse

import spark.Request

class AmbariStatusResponse : ITResponse() {

    private var status = AmbariClusterStatus.STOPPED

    @Throws(Exception::class)
    override fun handle(request: Request, response: spark.Response): Any {
        response.type("text/plain")
        return ambariStatus()
    }

    fun ambariStatus(): String {
        val rootNode = JsonNodeFactory.instance.objectNode()

        val requests: Requests
        when (status) {
            AmbariStatusResponse.AmbariClusterStatus.STOPPED -> {
                requests = Requests(1, "STARTED", 0)
                status = AmbariClusterStatus.IN_PROGRESS
            }
            AmbariStatusResponse.AmbariClusterStatus.IN_PROGRESS -> {
                requests = Requests(2, "STARTED", 50)
                status = AmbariClusterStatus.STARTED
            }
            else -> requests = Requests(3, "SUCCESSFUL", 100)
        }

        return rootNode.set("Requests", objectMapper.valueToTree<JsonNode>(requests)).toString()
    }

    private enum class AmbariClusterStatus {
        STOPPED,
        IN_PROGRESS,
        STARTED
    }
}
