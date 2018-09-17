package com.sequenceiq.it.spark.ambari;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.cloudbreak.newway.mock.DefaultModel;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.spark.StatefulRoute;
import com.sequenceiq.it.util.HostNameUtil;

import spark.Request;
import spark.Response;

public class AmbariClustersHostsResponseW extends ITResponse implements StatefulRoute {

    private Map<String, CloudVmMetaDataStatus> instanceMap;

    private final String state;

    public AmbariClustersHostsResponseW(String state) {
        this.state = state;
    }

    public AmbariClustersHostsResponseW(Map<String, CloudVmMetaDataStatus> instanceMap, String state) {
        this.instanceMap = instanceMap;
        this.state = state;
    }

    @Override
    public Object handle(Request request, Response response, DefaultModel model) throws Exception {
        this.instanceMap = model.getInstanceMap();
        return handle(request, response);
    }

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        rootNode.put("href", request.url() + "?fields=host_components/HostRoles/state,host_components/component/ServiceComponentInfo/category");

        ArrayNode items = rootNode.putArray("items");

        //for (Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : instanceMap.entrySet())
        instanceMap.forEach((key, value) -> {
            CloudVmMetaDataStatus status = value;
            ObjectNode item = items.addObject();
            String clusterName = request.params(":cluster");
            String hostName = HostNameUtil.generateHostNameByIp(status.getMetaData().getPrivateIp());
            item.putObject("Hosts")
                    .put("host_name", hostName)
                    .put("cluster_name", clusterName);
            ArrayNode hostComponents = item.putArray("host_components");
            ObjectNode component = hostComponents.addObject();
            component
                    .putObject("HostRoles")
                    .put("component_name", "DATANODE")
                    .put("cluster_name", clusterName)
                    .put("host_name", hostName)
                    .put("state", state);
            ArrayNode componentArray = component.putArray("component");
            componentArray.addObject()
                    .putObject("ServiceComponentInfo")
                    .put("component_name", "DATANODE")
                    .put("category", "MASTER");
            component = hostComponents.addObject();
            component
                    .putObject("HostRoles")
                    .put("component_name", "NODEMANAGER")
                    .put("cluster_name", clusterName)
                    .put("host_name", hostName)
                    .put("state", state);
            componentArray = component.putArray("component");
            componentArray.addObject()
                    .putObject("ServiceComponentInfo")
                    .put("component_name", "NODEMANAGER")
                    .put("category", "MASTER");
            // ---

//            String hostName = HostNameUtil.generateHostNameByIp(value.getMetaData().getPrivateIp());
//
//            ObjectNode item = items.addObject();
//            ObjectNode hosts = item.putObject("Hosts");
//            String clusterName = request.params(":cluster");
//            hosts.put("cluster_name", clusterName);
//            hosts.put("host_name", hostName);
//            ArrayNode hostComponents = item.putArray("host_components");
//
//            ObjectNode hostComponent = hostComponents.addObject();
//
//            ObjectNode hostRoles1 = hostComponent.putObject("HostRoles");
//            hostRoles1.put("component_name", "DATANODE");
//            hostRoles1.put("cluster_name", clusterName);
//            hostRoles1.put("host_name", hostName);
//            hostRoles1.put("state", "STARTED");
//
//            ArrayNode componentArray = hostComponent.putArray("component");
//            ObjectNode component = componentArray.addObject();
//            ObjectNode serviceComponentInfo = component.putObject("ServiceComponentInfo");
//            serviceComponentInfo.put("component_name", "DATANODE");
//            serviceComponentInfo.put("category", "MASTER");
//            }
        });
        return rootNode;
    }
}
