package com.sequenceiq.it.spark.ambari;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.spark.ambari.model.Clusters;
import com.sequenceiq.it.spark.ambari.model.Hosts;
import com.sequenceiq.it.util.HostNameUtil;

import spark.Request;
import spark.Response;

public class AmbariClusterResponse extends ITResponse {

    private final Map<String, CloudVmMetaDataStatus> instanceMap;

    public AmbariClusterResponse(Map<String, CloudVmMetaDataStatus> instanceMap) {
        this.instanceMap = instanceMap;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();

        Set<String> hostNames = instanceMap.values().stream()
                .map(cv -> HostNameUtil.generateHostNameByIp(cv.getMetaData().getPrivateIp())).collect(Collectors.toSet());
        rootNode.putObject("hosts")
                .set("Hosts", getObjectMapper().valueToTree(new Hosts(hostNames, "HEALTHY")));

        ArrayNode items = rootNode.putArray("items");
        items.addObject()
                .set("Clusters", getObjectMapper().valueToTree(new Clusters("ambari_cluster")));
        return rootNode;
    }
}
