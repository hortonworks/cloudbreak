package com.sequenceiq.it.spark.consul;

import com.ecwid.consul.v1.event.model.Event;
import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class ConsulEventFireResponse extends ITResponse {
    @Override
    public Object handle(Request request, Response response) throws Exception {
        Event event = new Event();
        event.setId("1");
        return event;
    }
}
