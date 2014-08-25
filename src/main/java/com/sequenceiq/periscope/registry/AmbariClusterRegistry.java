package com.sequenceiq.periscope.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;

@Component
public class AmbariClusterRegistry implements ClusterRegistry {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(AmbariClusterRegistry.class);
    private final Map<Long, Cluster> clusters = new ConcurrentHashMap<>();

    @Override
    public Cluster add(Cluster cluster) {
        // TODO should be per user registry
        long id = cluster.getId();
        clusters.put(id, cluster);
        LOGGER.info(id, "Cluster registered with ambari host: {}", cluster.getHost());
        return cluster;
    }

    @Override
    public Cluster remove(long id) {
        LOGGER.info(id, "Cluster removed from registry", id);
        return clusters.remove(id);
    }

    @Override
    public Cluster get(long id) {
        return clusters.get(id);
    }

    @Override
    public List<Cluster> getAll() {
        return new ArrayList<>(clusters.values());
    }
}
