package com.sequenceiq.it.spark.consul

import com.ecwid.consul.v1.event.model.Event
import com.sequenceiq.it.spark.ITResponse

import spark.Request
import spark.Response

class ConsulEventFireResponse : ITResponse() {
    @Throws(Exception::class)
    override fun handle(request: Request, response: Response): Any {
        val event = Event()
        event.id = "1"
        return event
    }
}
