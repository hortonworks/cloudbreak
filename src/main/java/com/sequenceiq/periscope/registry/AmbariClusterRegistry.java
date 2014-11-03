package com.sequenceiq.periscope.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;

@Component
public class AmbariClusterRegistry implements ClusterRegistry {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(AmbariClusterRegistry.class);
    private final Map<PeriscopeUser, Map<Long, Cluster>> userClusters = new ConcurrentHashMap<>();
    private final Map<Long, Cluster> clusters = new ConcurrentHashMap<>();

    @Override
    public Cluster add(PeriscopeUser user, Cluster cluster) {
        long id = cluster.getId();
        Map<Long, Cluster> clusterMap = getUserClusters(user);
        if (clusterMap == null) {
            clusterMap = new ConcurrentHashMap<>();
        }
        clusters.put(id, cluster);
        clusterMap.put(id, cluster);
        userClusters.put(user, clusterMap);
        LOGGER.info(id, "Cluster registered with ambari host: {}", cluster.getHost());
        return cluster;
    }

    @Override
    public Cluster remove(PeriscopeUser user, long id) {
        Map<Long, Cluster> clusterMap = getUserClusters(user);
        if (clusterMap != null) {
            clusters.remove(id);
            return clusterMap.remove(id);
        }
        return null;
    }

    @Override
    public Cluster get(PeriscopeUser user, long id) {
        Map<Long, Cluster> clusterMap = getUserClusters(user);
        if (clusterMap != null) {
            return clusterMap.get(id);
        }
        return null;
    }

    @Override
    public Cluster get(long id) {
        return clusters.get(id);
    }

    @Override
    public List<Cluster> getAll() {
        return new ArrayList<>(clusters.values());
    }

    private Map<Long, Cluster> getUserClusters(PeriscopeUser user) {
        return userClusters.get(user);
    }
}
