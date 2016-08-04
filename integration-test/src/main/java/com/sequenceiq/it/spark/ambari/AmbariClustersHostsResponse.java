package com.sequenceiq.it.spark.ambari;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.spark.ambari.model.Hosts;

import spark.Request;
import spark.Response;

public class AmbariClustersHostsResponse extends ITResponse {
    private int serverNumber;

    public AmbariClustersHostsResponse(int serverNumber) {
        this.serverNumber = serverNumber;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        ArrayNode items = rootNode.putArray("items");

        for (int i = 1; i <= serverNumber; i++) {
            Hosts hosts = new Hosts("host" + i, "HEALTHY");
            ObjectNode item = items.addObject();
            item.set("Hosts", getObjectMapper().valueToTree(hosts));

            item.putArray("host_components")
                    .addObject()
                    .putObject("HostRoles")
                    .put("component_name", "component-name")
                    .put("state", "SUCCESSFUL");
        }
        return rootNode;
    }
}
