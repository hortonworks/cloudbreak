package com.sequenceiq.it.spark.ambari;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.cloudbreak.newway.mock.DefaultModel;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.spark.StatefulRoute;
import com.sequenceiq.it.spark.ambari.model.Clusters;
import com.sequenceiq.it.spark.ambari.model.Hosts;
import com.sequenceiq.it.util.HostNameUtil;

import spark.Request;
import spark.Response;

public class AmbariClusterResponse extends ITResponse implements StatefulRoute {

    private String clusterName;

    private Map<String, CloudVmMetaDataStatus> instanceMap;

    public AmbariClusterResponse() {
    }

    public AmbariClusterResponse(Map<String, CloudVmMetaDataStatus> instanceMap) {
        this(instanceMap, "ambari_cluster");
    }

    public AmbariClusterResponse(Map<String, CloudVmMetaDataStatus> instanceMap, String clusterName) {
        this.clusterName = clusterName;
        this.instanceMap = instanceMap;
    }

    @Override
    public Object handle(Request request, Response response, DefaultModel model) {
        clusterName = model.getClusterName();
        instanceMap = model.getInstanceMap();
        return handle(request, response);
    }

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();

        Set<String> hostNames = instanceMap.values().stream()
                .map(cv -> HostNameUtil.generateHostNameByIp(cv.getMetaData().getPrivateIp())).collect(Collectors.toSet());
        rootNode.putObject("hosts")
                .set("Hosts", getObjectMapper().valueToTree(new Hosts(hostNames, "HEALTHY")));

        ArrayNode items = rootNode.putArray("items");
        items.addObject()
                .set("Clusters", getObjectMapper().valueToTree(new Clusters(clusterName)));
        return rootNode;
    }
}
