package com.sequenceiq.periscope.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.model.Ambari;
import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.registry.ClusterRegistry;
import com.sequenceiq.periscope.registry.ClusterState;
import com.sequenceiq.periscope.registry.ConnectionException;

@Service
public class ClusterService {

    @Autowired
    private ClusterRegistry clusterRegistry;

    public Cluster get(String clusterId) {
        return clusterRegistry.get(clusterId);
    }

    public Cluster add(String clusterId, Ambari ambari) throws ConnectionException {
        return clusterRegistry.add(clusterId, ambari);
    }

    public Collection<Cluster> getAll() {
        return clusterRegistry.getAll();
    }

    public Cluster remove(String clusterId) {
        return clusterRegistry.remove(clusterId);
    }

    public Cluster setState(String clusterId, ClusterState state) {
        Cluster cluster = get(clusterId);
        if (cluster != null) {
            cluster.setState(state);
        }
        return cluster;
    }

    public Cluster refreshConfiguration(String clusterId) throws ConnectionException {
        Cluster cluster = get(clusterId);
        if (cluster != null) {
            cluster.refreshConfiguration();
        }
        return cluster;
    }

}
