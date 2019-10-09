package com.sequenceiq.it.cloudbreak.mock.ambari;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariClusterRequestResponse extends ITResponse {

    private int idCounter;

    private final String ipAddress;

    private final String clusterName;

    public AmbariClusterRequestResponse(String ipAddress, String clusterName) {
        this.ipAddress = ipAddress;
        this.clusterName = clusterName;
    }

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        int id = getNextId();
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        rootNode.put("href", createUrl(ipAddress, clusterName, id));
        rootNode.putObject("Requests")
                .put("id", id)
                .put("status", "Accepted");

        return rootNode;
    }

    private String createUrl(String ipAddress, String clusterName, int id) {
        return String.format("https://%s:443/%s/dp-proxy/ambari/api/v1/clusters/%s/requests/%d", ipAddress, clusterName, clusterName, id);
    }

    private int getNextId() {
        ++idCounter;
        return idCounter;
    }
}
