package com.sequenceiq.it.spark.ambari;

import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariCheckResponse extends ITResponse {
    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        return "RUNNING";
    }
}
