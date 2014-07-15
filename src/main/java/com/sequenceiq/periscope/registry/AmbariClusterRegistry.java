package com.sequenceiq.periscope.registry;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.Ambari;

@Component
public class AmbariClusterRegistry implements ClusterRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterRegistry.class);
    private Map<String, Cluster> clusters = new ConcurrentHashMap<>();

    @Override
    public Cluster add(String id, Ambari ambari) throws ConnectionException {
        // TODO should be per user registry
        Cluster cluster = new Cluster(id, ambari);
        clusters.put(id, cluster);
        LOGGER.info("Cluster: {} registered with id: {}", ambari.getHost(), id);
        return cluster;
    }

    @Override
    public Cluster remove(String id) {
        LOGGER.info("Cluster: {} removed from registry", id);
        return clusters.remove(id);
    }

    @Override
    public Cluster get(String id) {
        return clusters.get(id);
    }

    @Override
    public Collection<Cluster> getAll() {
        return clusters.values();
    }
}
