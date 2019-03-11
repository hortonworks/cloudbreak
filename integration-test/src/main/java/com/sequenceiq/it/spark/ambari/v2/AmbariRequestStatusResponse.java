package com.sequenceiq.it.spark.ambari.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.spark.ambari.model.Requests;

import spark.Request;
import spark.Response;

public class AmbariRequestStatusResponse extends ITResponse {

    private int requestId;

    private int percentage;

    public AmbariRequestStatusResponse(int requestId, int percentage) {
        this.requestId = requestId;
        this.percentage = percentage;
    }

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        Requests requests = new Requests(requestId, "SUCCESSFUL", percentage);
        rootNode.set("Requests", new ObjectMapper().valueToTree(requests));
        return rootNode.toString();
    }
}
