package com.sequenceiq.it.spark.ambari

import com.sequenceiq.it.spark.ambari.model.RootServiceComponents
import com.sequenceiq.it.spark.ambari.model.Services
import com.sequenceiq.it.spark.ITResponse

import spark.Request
import spark.Response

class AmbariServicesComponentsResponse : ITResponse() {
    @Throws(Exception::class)
    override fun handle(request: Request, response: Response): Any {
        response.type("text/plain")
        return Services(RootServiceComponents("2.2.2"))
    }
}
