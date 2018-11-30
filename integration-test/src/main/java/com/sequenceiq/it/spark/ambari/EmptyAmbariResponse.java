package com.sequenceiq.it.spark.ambari;

import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class EmptyAmbariResponse extends ITResponse {

    private final int statusCode;

    public EmptyAmbariResponse() {
        this.statusCode = 200;
    }

    public EmptyAmbariResponse(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        response.status(statusCode);
        return "";
    }
}
