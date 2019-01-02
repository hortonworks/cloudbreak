package com.sequenceiq.it.spark.ambari;

import static com.sequenceiq.it.cloudbreak.newway.Mock.gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.sequenceiq.it.cloudbreak.newway.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.newway.mock.model.AmbariMock;
import com.sequenceiq.it.spark.StatefulRoute;

import spark.Request;
import spark.Response;

public class AmbariHostComponents implements StatefulRoute {
    @Override
    public Object handle(Request request, Response response, DefaultModel model) {
        response.type("text/plain");
        response.type("text/plain");
        List<Map<String, ?>> itemList = createItemList(model,
                Lists.newArrayList("ZOOKEEPER_CLIENT", "ZOOKEEPER_SERVER", "RANGER_ADMIN", "RANGER_TAGSYNC", "RANGER_USERSYNC", "KERBEROS_CLIENT",
                        "INFRA_SOLR", "INFRA_SOLR_CLIENT"));

        return gson().toJson(Collections.singletonMap("items", itemList));
    }

    private List<Map<String, ?>> createItemList(DefaultModel model, Iterable<String> components) {
        List<Map<String, ?>> itemList = new ArrayList<>();
        for (String component : components) {
            Map<String, Object> item = new HashMap<>();
            Map<String, String> serviceInfo = new HashMap<>();
            serviceInfo.put("cluster_name", model.getClusterName());
            serviceInfo.put("host_name", model.getMockServerAddress());
            serviceInfo.put("component_name", component);
            item.put("HostRoles", serviceInfo);
            item.put("href", "http://" + model.getMockServerAddress()
                    + AmbariMock.CLUSTERS_CLUSTER_SERVICES_ROOT.replaceAll(":cluster", model.getClusterName()) + "/hosts/" + model.getMockServerAddress()
                    + "/host_components/" + component);
            item.put("host", Collections.singletonMap("href", "http://" + model.getMockServerAddress()
                    + AmbariMock.CLUSTERS_CLUSTER_SERVICES_ROOT.replaceAll(":cluster", model.getClusterName())
                    + "/hosts/" + model.getMockServerAddress()));
            itemList.add(item);
        }
        return itemList;
    }
}
