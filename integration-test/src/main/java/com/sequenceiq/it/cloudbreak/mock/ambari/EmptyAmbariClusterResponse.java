package com.sequenceiq.it.cloudbreak.mock.ambari;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;

import spark.Request;
import spark.Response;

public class EmptyAmbariClusterResponse extends ITResponse {

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        ArrayNode items = rootNode.putArray("items");
        items.addObject().set("Clusters", null);
        return rootNode;
    }
}
