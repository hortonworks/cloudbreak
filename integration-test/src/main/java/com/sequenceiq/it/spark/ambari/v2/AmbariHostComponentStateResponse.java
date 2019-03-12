package com.sequenceiq.it.spark.ambari.v2;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.util.HostNameUtil;

import spark.Request;
import spark.Response;

public class AmbariHostComponentStateResponse extends ITResponse {

    private final Map<String, CloudVmMetaDataStatus> instanceMap;

    public AmbariHostComponentStateResponse(Map<String, CloudVmMetaDataStatus> instanceMap) {
        this.instanceMap = instanceMap;
    }

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        rootNode.put("href", request.url() + "?fields=host_components/HostRoles/state/*");
        ArrayNode items = rootNode.putArray("items");

        instanceMap.forEach((key, value) -> {
            String hostName = HostNameUtil.generateHostNameByIp(value.getMetaData().getPrivateIp());

            ObjectNode item = items.addObject();
            ObjectNode hosts = item.putObject("Hosts");
            String clusterName = request.params(":cluster");
            hosts.put("cluster_name", clusterName);
            hosts.put("host_name", hostName);
            ArrayNode hostComponents = item.putArray("host_components");

            ObjectNode hostComponent = hostComponents.addObject();

            ObjectNode hostRoles1 = hostComponent.putObject("HostRoles");
            hostRoles1.put("component_name", "DATANODE");
            hostRoles1.put("cluster_name", clusterName);
            hostRoles1.put("host_name", hostName);
            hostRoles1.put("state", "STARTED");

            ObjectNode hostRoles2 = hostComponent.putObject("HostRoles");
            hostRoles2.put("component_name", "NODEMANAGER");
            hostRoles2.put("cluster_name", clusterName);
            hostRoles2.put("host_name", hostName);
            hostRoles2.put("state", "STARTED");
        });

        return rootNode;
    }
}
