package com.sequenceiq.it.spark.ambari;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.spark.ambari.model.Clusters;
import com.sequenceiq.it.spark.ambari.model.Hosts;

import spark.Request;
import spark.Response;

public class AmbariClusterResponse extends ITResponse {

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();

        rootNode.putObject("hosts")
                .set("Hosts", getObjectMapper().valueToTree(new Hosts("127.0.0.1", "HEALTHY")));

        ArrayNode items = rootNode.putArray("items");
        items.addObject()
                .set("Clusters", getObjectMapper().valueToTree(new Clusters("ambari_cluster")));
        return rootNode;
    }
}
