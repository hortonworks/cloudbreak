package com.sequenceiq.it.spark.ambari;

import java.util.Map;
import java.util.Set;

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

    private Set<String> removedHostsFromAmbari;

    private final String state;

    public AmbariClustersHostsResponseW(String state) {
        this.state = state;
    }

    @Override
    public Object handle(Request request, Response response, DefaultModel model) {
        this.instanceMap = model.getInstanceMap();
        this.removedHostsFromAmbari = model.getRemovedHostsFromAmbari();
        return handle(request, response);
    }

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        rootNode.put("href", request.url() + "?fields=host_components/HostRoles/state,host_components/component/ServiceComponentInfo/category");

        ArrayNode items = rootNode.putArray("items");
        String hostsParam = request.queryParams().stream().filter(qp -> qp.contains("host_name.in")).findFirst().orElse("");

        instanceMap.entrySet().stream()
                .filter(e -> hostsParam.isEmpty() || hostsParam.contains(e.getValue().getMetaData().getPrivateIp().replaceAll("\\.", "-") + ".")
                        && !removedHostsFromAmbari.contains(e.getKey()))
                .forEach(e -> {
                    CloudVmMetaDataStatus status = e.getValue();
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
                            .put("state", state)
                            .put("desired_admin_state", "INSERVICE");
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
                            .put("state", state)
                            .put("desired_admin_state", "INSERVICE");
                    componentArray = component.putArray("component");
                    componentArray.addObject()
                            .putObject("ServiceComponentInfo")
                            .put("component_name", "NODEMANAGER")
                            .put("category", "MASTER");
                });
        return rootNode;
    }
}
