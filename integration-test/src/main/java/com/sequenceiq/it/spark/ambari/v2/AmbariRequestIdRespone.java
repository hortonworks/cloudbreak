package com.sequenceiq.it.spark.ambari.v2;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariRequestIdRespone extends ITResponse {

    private int requestId;

    public AmbariRequestIdRespone(int requestId) {
        this.requestId = requestId;
    }

    @Override
    public Object handle(Request request, Response response) {

        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        ObjectNode requests = rootNode.putObject("Requests");
        requests.put("id", requestId);
        requests.put("status", "Accepted");
        return rootNode;
    }
}
