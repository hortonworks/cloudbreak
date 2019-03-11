package com.sequenceiq.it.spark.ambari.v2;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.util.HostNameUtil;

import spark.Request;
import spark.Response;

public class AmbariClustersHostsResponse extends ITResponse {

    private final Map<String, CloudVmMetaDataStatus> instanceMap;

    private final String state;

    public AmbariClustersHostsResponse(Map<String, CloudVmMetaDataStatus> instanceMap, String state) {
        this.instanceMap = instanceMap;
        this.state = state;
    }

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        ArrayNode items = rootNode.putArray("hosts");

        for (Map.Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : instanceMap.entrySet()) {
            CloudVmMetaDataStatus status = stringCloudVmMetaDataStatusEntry.getValue();
            if (InstanceStatus.STARTED == status.getCloudVmInstanceStatus().getStatus()) {
                ObjectNode item = items.addObject();
                item.putObject("Hosts").put("host_name", HostNameUtil.generateHostNameByIp(status.getMetaData().getPrivateIp()));
                ArrayNode components = item.putArray("host_components");
                components.addObject()
                        .putObject("HostRoles")
                        .put("component_name", "DATANODE")
                        .put("state", state);
                components.addObject()
                        .putObject("HostRoles")
                        .put("component_name", "NODEMANAGER")
                        .put("state", state);
            }
        }
        return rootNode;
    }
}
