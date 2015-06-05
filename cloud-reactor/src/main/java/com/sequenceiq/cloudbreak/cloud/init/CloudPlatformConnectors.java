package com.sequenceiq.cloudbreak.cloud.init;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;

@Component("CloudPlatformConnectorsV2")
public class CloudPlatformConnectors {

    @Inject
    private List<CloudConnector> connectors;

    private Map<String, CloudConnector> map;


    @PostConstruct
    public void cloudPlatformConnectors() {
        map = new HashMap<>();
        for (CloudConnector connector : connectors) {
            map.put(connector.platform(), connector);
        }
    }

    public CloudConnector get(String key) {
        return map.get(key);
    }

}
