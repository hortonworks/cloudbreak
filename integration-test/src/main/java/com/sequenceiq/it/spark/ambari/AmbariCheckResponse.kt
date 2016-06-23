package com.sequenceiq.it.spark.ambari

import com.sequenceiq.it.spark.ITResponse

import spark.Request
import spark.Response

class AmbariCheckResponse : ITResponse() {
    @Throws(Exception::class)
    override fun handle(request: Request, response: Response): Any {
        response.type("text/plain")
        return "RUNNING"
    }
}
