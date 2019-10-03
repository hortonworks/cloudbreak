package com.sequenceiq.it.spark.ambari;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.cloudbreak.newway.mock.DefaultModel;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.spark.StatefulRoute;
import com.sequenceiq.it.spark.ambari.model.Requests;

import spark.Request;
import spark.Response;

public class AmbariClusterHostDeleteRequestsResponse extends ITResponse implements StatefulRoute {
    private Map<String, CloudVmMetaDataStatus> instanceMap = new HashMap<>();

    private Set<String> removedHostsFromAmbari = new HashSet<>();

    public AmbariClusterHostDeleteRequestsResponse() {
    }

    public AmbariClusterHostDeleteRequestsResponse(Map<String, CloudVmMetaDataStatus> instanceMap, Set<String> removedHostsFromAmbari) {
        this.instanceMap = instanceMap;
        this.removedHostsFromAmbari = removedHostsFromAmbari;
    }

    @Override
    public Object handle(Request request, Response response, DefaultModel model) {
        instanceMap = model.getInstanceMap();
        removedHostsFromAmbari = model.getRemovedHostsFromAmbari();
        return handle(request, response);
    }

    @Override
    public Object handle(Request request, Response response) {
        try {
            String hostsParam = new ObjectMapper().readTree(request.body()).get("RequestInfo").get("query").asText();
            removedHostsFromAmbari.addAll(instanceMap.entrySet().stream().filter(e -> hostsParam.contains(
                    e.getValue().getMetaData().getPrivateIp().replaceAll("\\.", "-") + ".")).map(Map.Entry::getKey).collect(Collectors.toSet()));
            response.type("text/plain");
            ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
            Requests requests = new Requests(66, "SUCCESSFUL", 100);
            rootNode.set("Requests", new ObjectMapper().valueToTree(requests));
            return rootNode.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
