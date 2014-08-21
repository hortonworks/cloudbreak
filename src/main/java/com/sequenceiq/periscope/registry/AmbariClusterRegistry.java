package com.sequenceiq.periscope.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.ClusterDetails;
import com.sequenceiq.periscope.model.Cluster;

@Component
public class AmbariClusterRegistry implements ClusterRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterRegistry.class);
    private final Map<Long, Cluster> clusters = new ConcurrentHashMap<>();

    @Override
    public Cluster add(ClusterDetails clusterDetails) throws ConnectionException {
        // TODO should be per user registry
        long id = clusterDetails.getId();
        Cluster cluster = new Cluster(clusterDetails);
        clusters.put(id, cluster);
        LOGGER.info("Cluster: {} registered with id: {}", cluster.getHost(), id);
        return cluster;
    }

    @Override
    public Cluster remove(long id) {
        LOGGER.info("Cluster: {} removed from registry", id);
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
