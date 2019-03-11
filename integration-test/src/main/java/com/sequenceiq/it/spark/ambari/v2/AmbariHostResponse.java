package com.sequenceiq.it.spark.ambari.v2;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariHostResponse extends ITResponse {

    public static final String HEALTHY = "HEALTHY";

    public static final String UNHEALTHY = "UNHEALTHY";

    public static final String UNKNOWN = "UNKNOWN";

    private String hostStatus;

    public AmbariHostResponse() {
        this(HEALTHY);
    }

    public AmbariHostResponse(String hostStatus) {
        this.hostStatus = hostStatus;
    }

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        rootNode.putObject("Hosts").put("host_status", hostStatus);
        rootNode.put("href", request.url());
        return rootNode;
    }
}
