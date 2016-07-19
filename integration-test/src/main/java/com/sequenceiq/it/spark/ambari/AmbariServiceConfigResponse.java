package com.sequenceiq.it.spark.ambari;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariServiceConfigResponse extends ITResponse {

    private String resourceManagerAddress;

    public AmbariServiceConfigResponse(String resourceManagerAddress) {
        this.resourceManagerAddress = resourceManagerAddress;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("text/plain");
        List<Map<String, ?>> itemList = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("group_name", "default");
        List<Map> configList = new ArrayList<>();

        Map<String, Object> dfsReplication = new HashMap<>();
        dfsReplication.put("type", "something");
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("dfs.replication", "2");
        propertyMap.put("dfs.namenode.http-address", resourceManagerAddress + ":" + resourceManagerAddress);
        propertyMap.put("dfs.namenode.secondary.http-address", resourceManagerAddress + ":" + resourceManagerAddress);
        propertyMap.put("yarn.resourcemanager.webapp.address", resourceManagerAddress + ":" + resourceManagerAddress);
        dfsReplication.put("properties", propertyMap);
        configList.add(dfsReplication);

        map.put("configurations", configList);
        itemList.add(map);

        return Collections.singletonMap("items", itemList);
    }
}
