package com.sequenceiq.it.spark.ambari;

import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.spark.ambari.model.RootServiceComponents;
import com.sequenceiq.it.spark.ambari.model.Services;

import spark.Request;
import spark.Response;

public class AmbariServicesComponentsResponse extends ITResponse {
    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        return new Services(new RootServiceComponents("2.2.2"));
    }
}
