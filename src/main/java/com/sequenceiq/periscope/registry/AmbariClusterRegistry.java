package com.sequenceiq.periscope.registry;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.AmbariServer;

@Component
public class AmbariClusterRegistry implements ClusterRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterRegistry.class);
    private Map<String, ClusterRegistration> clusters = new ConcurrentHashMap<>();

    @Override
    public ClusterRegistration add(String id, AmbariServer ambariServer) {
        ClusterRegistration clusterRegistration = new ClusterRegistration(id, ambariServer);
        clusters.put(id, clusterRegistration);
        LOGGER.info("Cluster: {} registered with id: {}", ambariServer.getHost(), id);
        return clusterRegistration;
    }

    @Override
    public ClusterRegistration remove(String id) {
        LOGGER.info("Cluster: {} removed from registry", id);
        return clusters.remove(id);
    }

    @Override
    public ClusterRegistration get(String id) {
        return clusters.get(id);
    }

    @Override
    public Collection<ClusterRegistration> getAll() {
        return clusters.values();
    }
}
