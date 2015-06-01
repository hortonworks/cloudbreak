package com.sequenceiq.cloudbreak.cloud;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

@Component("CloudPlatformConnectorsV2")
public class CloudPlatformConnectors {

    @Inject
    private List<CloudPlatformConnectorV2> connectors;

    private Map<String, CloudPlatformConnectorV2> map;


    @PostConstruct
    public void cloudPlatformConnectors() {
        map = new HashMap<>();
        for (CloudPlatformConnectorV2 connector : connectors) {
            map.put(connector.getCloudPlatform(), connector);
        }
    }

    public CloudPlatformConnectorV2 get(String key) {
        return map.get(key);
    }

}
