package com.sequenceiq.it.spark.ambari;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.spark.ambari.model.Clusters;
import com.sequenceiq.it.spark.ambari.model.Hosts;
import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariClusterResponse extends ITResponse {

    private int serverNumber;

    public AmbariClusterResponse(int serverNumber) {
        this.serverNumber = serverNumber;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();

        List<String> ambariServers = new ArrayList<>();
        for (int i = 1; i <= sizeOfAmbariServers(serverNumber); i++) {
            ambariServers.add("127.0.0." + i);
        }

        rootNode.putObject("hosts")
                .set("Hosts", getObjectMapper().valueToTree(new Hosts(ambariServers, "HEALTHY")));

        ArrayNode items = rootNode.putArray("items");
        items.addObject()
                .set("Clusters", getObjectMapper().valueToTree(new Clusters("ambari_cluster")));
        return rootNode;
    }

    private static int sizeOfAmbariServers(int serverNumber) {
        return serverNumber - 1;
    }
}
