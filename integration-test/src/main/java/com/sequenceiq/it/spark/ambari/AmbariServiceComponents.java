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

public class AmbariServiceComponents implements StatefulRoute {
    @Override
    public Object handle(Request request, Response response, DefaultModel model) {
        response.type("text/plain");
        String serviceName = request.params("servicename");
        response.type("text/plain");
        List<Map<String, ?>> itemList;
        switch (serviceName) {
            case "AMBARI_INFRA_SOLR":
                itemList = createItemList(model, serviceName, Lists.newArrayList("INFRA_SOLR", "INFRA_SOLR_CLIENT"));
                break;
            case "KERBEROS":
                itemList = createItemList(model, serviceName, Lists.newArrayList("KERBEROS_CLIENT"));
                break;
            case "RANGER":
                itemList = createItemList(model, serviceName, Lists.newArrayList("RANGER_ADMIN", "RANGER_TAGSYNC", "RANGER_USERSYNC"));
                break;
            case "ZOOKEEPER":
                itemList = createItemList(model, serviceName, Lists.newArrayList("ZOOKEEPER_CLIENT", "ZOOKEEPER_SERVER"));
                break;
            default:
                itemList = Collections.emptyList();
                break;
        }

        return gson().toJson(Collections.singletonMap("items", itemList));
    }

    private List<Map<String, ?>> createItemList(DefaultModel model, String serviceName, Iterable<String> components) {
        List<Map<String, ?>> itemList = new ArrayList<>();
        for (String component : components) {
            Map<String, Object> item = new HashMap<>();
            Map<String, String> serviceInfo = new HashMap<>();
            serviceInfo.put("cluster_name", model.getClusterName());
            serviceInfo.put("service_name", serviceName);
            serviceInfo.put("component_name", component);
            item.put("ServiceComponentInfo", serviceInfo);
            item.put("href", "http://" + model.getMockServerAddress()
                    + AmbariMock.CLUSTERS_CLUSTER_SERVICES_ROOT.replaceAll(":cluster", model.getClusterName()) + "/" + serviceName
                    + "/components/" + component);
            itemList.add(item);
        }
        return itemList;
    }
}
