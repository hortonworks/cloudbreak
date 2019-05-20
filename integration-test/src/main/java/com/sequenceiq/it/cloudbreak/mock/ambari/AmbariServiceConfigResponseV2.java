package com.sequenceiq.it.cloudbreak.mock.ambari;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.it.cloudbreak.Mock;
import com.sequenceiq.it.cloudbreak.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.spark.StatefulRoute;

import spark.Request;
import spark.Response;

public class AmbariServiceConfigResponseV2 implements StatefulRoute {

    @Override
    public Object handle(Request request, Response response, DefaultModel model) {
        response.type("text/plain");
        List<Map<String, ?>> itemList = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("group_name", "default");
        Collection<Map<String, Object>> configList = new ArrayList<>();

        Map<String, Object> dfsReplication = new HashMap<>();
        dfsReplication.put("type", "something");
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("dfs.replication", "2");
        propertyMap.put("dfs.namenode.http-address", model.getMockServerAddress() + ':' + model.getSshPort());
        propertyMap.put("dfs.namenode.secondary.http-address", model.getMockServerAddress() + ':' + model.getSshPort());
        propertyMap.put("yarn.resourcemanager.webapp.address", model.getMockServerAddress() + ':' + model.getSshPort());
        dfsReplication.put("properties", propertyMap);
        configList.add(dfsReplication);

        map.put("configurations", configList);
        itemList.add(map);

        return Mock.gson().toJson(Collections.singletonMap("items", itemList));
    }
}
