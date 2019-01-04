package com.sequenceiq.cloudbreak.service.cluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.cluster.api.ClusterApi;

@Service
public class ClusterApiConnectors {

    private final Map<String, ClusterApi> map = new HashMap<>();

    @Inject
    private List<ClusterApi> connectors;

    @PostConstruct
    public void connectors() {
        for (ClusterApi connector : connectors) {
            map.put(connector.clusterVariant(), connector);
        }
    }

    public ClusterApi getConnector(String variant) {
        return map.get(variant);
    }
}
