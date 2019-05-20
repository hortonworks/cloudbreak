package com.sequenceiq.it.cloudbreak.mock.ambari;

import com.sequenceiq.it.cloudbreak.mock.model.RootServiceComponents;
import com.sequenceiq.it.cloudbreak.mock.model.Services;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariServicesComponentsResponse extends ITResponse {
    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        return new Services(new RootServiceComponents("2.2.2"));
    }
}
