package com.sequenceiq.cloudbreak.cloud.init;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;

@Component
public class CloudPlatformConnectors {

    @Inject
    private List<CloudConnector> cloudConnectors;
    private Map<String, CloudConnector> map = new HashMap<>();

    @PostConstruct
    public void cloudPlatformConnectors() {
        for (CloudConnector connector : cloudConnectors) {
            map.put(connector.platform(), connector);
        }
    }

    public CloudConnector get(String key) {
        return map.get(key);
    }

}
