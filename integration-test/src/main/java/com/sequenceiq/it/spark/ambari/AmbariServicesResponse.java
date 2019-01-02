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

public class AmbariServicesResponse implements StatefulRoute {
    @Override
    public Object handle(Request request, Response response, DefaultModel model) {
        response.type("text/plain");
        List<Map<String, ?>> itemList = new ArrayList<>();
        for (String service : Lists.newArrayList("AMBARI_INFRA_SOLR", "KERBEROS", "RANGER", "ZOOKEEPER")) {
            Map<String, Object> item = new HashMap<>();
            Map<String, String> serviceInfo = new HashMap<>();
            serviceInfo.put("cluster_name", model.getClusterName());
            serviceInfo.put("service_name", service);
            item.put("ServiceInfo", serviceInfo);
            item.put("href", "http://" + model.getMockServerAddress()
                    + AmbariMock.CLUSTERS_CLUSTER_SERVICES_ROOT.replaceAll(":cluster", model.getClusterName()) + "/" + service);
            itemList.add(item);
        }

        return gson().toJson(Collections.singletonMap("items", itemList));
    }
}
