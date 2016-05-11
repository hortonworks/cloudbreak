package com.sequenceiq.it.spark.ambari;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.spark.ambari.model.Requests;
import com.sequenceiq.it.spark.ITResponse;

import spark.Request;

public class AmbariStatusResponse extends ITResponse {

    private AmbariClusterStatus status = AmbariClusterStatus.STOPPED;

    @Override
    public Object handle(Request request, spark.Response response) throws Exception {
        response.type("text/plain");
        return ambariStatus();
    }

    public String ambariStatus() {
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();

        Requests requests;
        switch (status) {
            case STOPPED:
                requests = new Requests(1, "STARTED", 0);
                status = AmbariClusterStatus.IN_PROGRESS;
                break;
            case IN_PROGRESS:
                requests = new Requests(2, "STARTED", 50);
                status = AmbariClusterStatus.STARTED;
                break;
            default:
                requests = new Requests(3, "SUCCESSFUL", 100);
                break;
        }

        return rootNode.set("Requests", getObjectMapper().valueToTree(requests)).toString();
    }

    private enum AmbariClusterStatus {
        STOPPED,
        IN_PROGRESS,
        STARTED
    }
}
