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

    private final String resourceManagerAddress;

    private final int port;

    public AmbariServiceConfigResponse(String resourceManagerAddress, int port) {
        this.resourceManagerAddress = resourceManagerAddress;
        this.port = port;
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
        propertyMap.put("dfs.namenode.http-address", resourceManagerAddress + ':' + port);
        propertyMap.put("dfs.namenode.secondary.http-address", resourceManagerAddress + ':' + port);
        propertyMap.put("yarn.resourcemanager.webapp.address", resourceManagerAddress + ':' + port);
        dfsReplication.put("properties", propertyMap);
        configList.add(dfsReplication);

        map.put("configurations", configList);
        itemList.add(map);

        return Collections.singletonMap("items", itemList);
    }
}
