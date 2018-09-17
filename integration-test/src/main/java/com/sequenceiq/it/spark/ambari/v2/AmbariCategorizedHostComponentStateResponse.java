package com.sequenceiq.it.spark.ambari.v2;

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

public class AmbariCategorizedHostComponentStateResponse extends ITResponse implements StatefulRoute {

    private Map<String, CloudVmMetaDataStatus> instanceMap;

    public AmbariCategorizedHostComponentStateResponse() {
    }

    public AmbariCategorizedHostComponentStateResponse(Map<String, CloudVmMetaDataStatus> instanceMap) {
        this.instanceMap = instanceMap;
    }

    @Override
    public Object handle(Request request, Response response, DefaultModel model) throws Exception {
        this.instanceMap = model.getInstanceMap();
        return handle(request, response);
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        rootNode.put("href", request.url() + "?fields=host_components/HostRoles/state,host_components/component/ServiceComponentInfo/category");
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

            ArrayNode componentArray = hostComponent.putArray("component");
            ObjectNode component = componentArray.addObject();
            ObjectNode serviceComponentInfo = component.putObject("ServiceComponentInfo");
            serviceComponentInfo.put("component_name", "DATANODE");
            serviceComponentInfo.put("category", "MASTER");
        });

        return rootNode;
    }
}
