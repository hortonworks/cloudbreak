package com.sequenceiq.it.cloudbreak.mock.ambari;

import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariGetHostComponentsReponse extends ITResponse {

    private final Set<String> components;

    private final String clusterName;

    public AmbariGetHostComponentsReponse(Set<String> components, String clusterName) {
        this.components = components;
        this.clusterName = clusterName;
    }

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        String url = request.url();
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        rootNode.put("href", url + "?fields=HostRoles");

        ArrayNode items = rootNode.putArray("items");
        components.forEach(comp -> addComponentNode(items, url, clusterName, comp));

        return rootNode;
    }

    private void addComponentNode(ArrayNode items, String url, String clusterName, String component) {
        ObjectNode item = items.addObject();
        item.put("href", url + "/" + component);
        ObjectNode hostRoleNode = item.putObject("HostRoles");
        hostRoleNode.put("cluster_name", clusterName);
        hostRoleNode.put("component_name", component);
        hostRoleNode.put("host_name", "host1");
        hostRoleNode.put("state", "INSTALLED");
        item.putObject("host").put("href", url);
    }
}
