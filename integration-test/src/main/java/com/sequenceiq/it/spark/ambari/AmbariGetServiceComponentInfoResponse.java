package com.sequenceiq.it.spark.ambari;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariGetServiceComponentInfoResponse extends ITResponse {

    private String componentName;

    public AmbariGetServiceComponentInfoResponse(String componentName) {
        this.componentName = componentName;
    }

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        rootNode.put("href", request.url() + "?fields=ServiceComponentInfo");
        rootNode.putObject("ServiceComponentInfo")
                .put("category", "MASTER")
                .put("component_name", componentName);

        return rootNode;
    }
}
