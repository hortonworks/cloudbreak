package com.sequenceiq.it.spark.ambari;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.spark.ambari.model.Hosts;

import spark.Request;
import spark.Response;

public class AmbariClustersHostsResponse extends ITResponse {
    private Map<String, CloudVmMetaDataStatus> instanceMap;

    public  AmbariClustersHostsResponse(Map<String, CloudVmMetaDataStatus> instanceMap) {
        this.instanceMap = instanceMap;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        ArrayNode items = rootNode.putArray("items");

        for (String instanceId : instanceMap.keySet()) {
            if (InstanceStatus.STARTED == instanceMap.get(instanceId).getCloudVmInstanceStatus().getStatus()) {
                Hosts hosts = new Hosts(Collections.singletonList("host" + instanceId), "HEALTHY");
                ObjectNode item = items.addObject();
                item.set("Hosts", getObjectMapper().valueToTree(hosts));

                item.putArray("host_components")
                        .addObject()
                        .putObject("HostRoles")
                        .put("component_name", "component-name")
                        .put("state", "SUCCESSFUL");
            }
        }
        return rootNode;
    }
}
