package com.sequenceiq.it.spark.ambari;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.spark.ambari.model.Clusters;
import com.sequenceiq.it.spark.ambari.model.Hosts;

import spark.Request;
import spark.Response;

public class AmbariClusterResponse extends ITResponse {

    private Map<String, CloudVmMetaDataStatus> instanceMap;

    public AmbariClusterResponse(Map<String, CloudVmMetaDataStatus> instanceMap) {
        this.instanceMap = instanceMap;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();

        rootNode.putObject("hosts")
                .set("Hosts", getObjectMapper().valueToTree(new Hosts(Collections.singletonList("127.0.0.1"), "HEALTHY")));

        ArrayNode items = rootNode.putArray("items");
        items.addObject()
                .set("Clusters", getObjectMapper().valueToTree(new Clusters("ambari_cluster")));
        return rootNode;
    }
}
