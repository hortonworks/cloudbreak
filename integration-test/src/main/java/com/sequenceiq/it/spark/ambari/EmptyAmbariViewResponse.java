package com.sequenceiq.it.spark.ambari;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class EmptyAmbariViewResponse extends ITResponse {
    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        ArrayNode items = rootNode.putArray("items");
        rootNode.set("items", items);
        return rootNode.toString();
    }
}
