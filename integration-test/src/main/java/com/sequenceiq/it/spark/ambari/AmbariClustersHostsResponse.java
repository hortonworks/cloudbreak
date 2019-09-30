package com.sequenceiq.it.spark.ambari;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

    private Set<String> removedHostsFromAmbari;

    private final String state;

    public AmbariClustersHostsResponse(Map<String, CloudVmMetaDataStatus> instanceMap, Set<String> removedHostsFromAmbari, String state) {
        this.instanceMap = instanceMap;
        this.removedHostsFromAmbari = removedHostsFromAmbari;
        this.state = state;
    }

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        ArrayNode items = rootNode.putArray("items");
        String hostsParam = request.queryParams().stream().filter(qp -> qp.contains("host_name.in")).findFirst().orElse("");
        for (Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : instanceMap.entrySet()) {
            CloudVmMetaDataStatus status = stringCloudVmMetaDataStatusEntry.getValue();
            if (hostsParam.isEmpty()
                    || hostsParam.contains(stringCloudVmMetaDataStatusEntry.getValue().getMetaData().getPrivateIp().replaceAll("\\.", "-") + ".")
                    && !removedHostsFromAmbari.contains(stringCloudVmMetaDataStatusEntry.getKey())
                    && InstanceStatus.STARTED == status.getCloudVmInstanceStatus().getStatus()) {
                ObjectNode item = items.addObject();
                String hostName = HostNameUtil.generateHostNameByIp(status.getMetaData().getPrivateIp());
                item.putObject("Hosts").put("host_name", hostName);
                ArrayNode components = item.putArray("host_components");
                ObjectNode dataNode = components.addObject();
                dataNode.putObject("HostRoles")
                        .put("component_name", "DATANODE")
                        .put("state", state)
                        .put("desired_admin_state", "INSERVICE");
                addComponentNode(dataNode, "SLAVE", "DATANODE", "HDF", hostName);

                ObjectNode nodeManagerNode = components.addObject();
                nodeManagerNode.putObject("HostRoles")
                        .put("component_name", "NODEMANAGER")
                        .put("state", state)
                        .put("desired_admin_state", "INSERVICE");
                addComponentNode(nodeManagerNode, "SLAVE", "NODEMANAGER", "YARN", hostName);
            }
        }
        return rootNode;
    }

    private void addComponentNode(ObjectNode dataNode, String category, String componentName, String serviceName, String hostName) {
        ArrayNode dataNodeComponents = dataNode.putArray("component");
        ObjectNode componentNode = dataNodeComponents.addObject();
        componentNode.putObject("ServiceComponentInfo")
                .put("category", category)
                .put("cluster_name", "clustername")
                .put("component_name", componentName)
                .put("service_name", serviceName);
    }
}