package com.sequenceiq.it.spark.ambari

import java.util.ArrayList
import java.util.Collections

import com.sequenceiq.it.spark.ambari.model.Hosts
import com.sequenceiq.it.spark.ITResponse

import spark.Request
import spark.Response

class AmbariHostsResponse(private val serverNumber: Int) : ITResponse() {

    @Throws(Exception::class)
    override fun handle(request: Request, response: Response): Any {
        response.type("text/plain")
        val itemList = ArrayList<Map<String, *>>()
        for (i in 1..serverNumber) {
            val hosts = Hosts(listOf<String>("host" + i), "HEALTHY")
            itemList.add(Collections.singletonMap("Hosts", hosts))
        }

        return Collections.singletonMap<String, List<Map<String, *>>>("items", itemList)
    }
}
