package com.sequenceiq.it.spark.ambari;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariVersionDefinitionResponse extends ITResponse {

    public  AmbariVersionDefinitionResponse() {
    }

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        rootNode.putArray("items");
        return rootNode;
    }
}
