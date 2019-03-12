package com.sequenceiq.it.spark.ambari.v2;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariStrRequestIdRespone extends ITResponse {

    private int requestId;

    public AmbariStrRequestIdRespone(int requestId) {
        this.requestId = requestId;
    }

    @Override
    public Object handle(Request request, Response response) {

        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        rootNode.put("str", "{\"Requests\":{\"id\":" + requestId + ",\"status\":\"Accepted\"}}");
        return rootNode;
    }
}
