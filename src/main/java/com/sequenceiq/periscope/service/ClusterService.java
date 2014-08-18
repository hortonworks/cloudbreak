package com.sequenceiq.periscope.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.model.Ambari;
import com.sequenceiq.periscope.model.QueueSetup;
import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.registry.ClusterRegistry;
import com.sequenceiq.periscope.registry.ClusterState;
import com.sequenceiq.periscope.registry.ConnectionException;
import com.sequenceiq.periscope.registry.QueueSetupException;

@Service
public class ClusterService {

    @Autowired
    private ClusterRegistry clusterRegistry;

    public Cluster get(String clusterId) throws ClusterNotFoundException {
        Cluster cluster = clusterRegistry.get(clusterId);
        if (cluster == null) {
            throw new ClusterNotFoundException(clusterId);
        }
        return cluster;
    }

    public Cluster add(String clusterId, Ambari ambari) throws ConnectionException {
        return clusterRegistry.add(clusterId, ambari);
    }

    public List<Cluster> getAll() {
        return clusterRegistry.getAll();
    }

    public Cluster remove(String clusterId) throws ClusterNotFoundException {
        Cluster cluster = clusterRegistry.remove(clusterId);
        if (cluster == null) {
            throw new ClusterNotFoundException(clusterId);
        }
        return cluster;
    }

    public Cluster setState(String clusterId, ClusterState state) throws ClusterNotFoundException {
        Cluster cluster = get(clusterId);
        cluster.setState(state);
        return cluster;
    }

    public Cluster refreshConfiguration(String clusterId) throws ConnectionException, ClusterNotFoundException {
        Cluster cluster = get(clusterId);
        cluster.refreshConfiguration();
        return cluster;
    }

    public Map<String, String> setQueueSetup(String clusterId, QueueSetup queueSetup)
            throws QueueSetupException, ClusterNotFoundException {
        return get(clusterId).setQueueSetup(queueSetup);
    }

}
