package com.sequenceiq.periscope.registry;

import java.util.List;

import com.sequenceiq.periscope.model.Ambari;
import com.sequenceiq.periscope.model.Cluster;

public interface ClusterRegistry {

    /**
     * Adds a new hadoop cluster to the registry.
     *
     * @param id     id of the cluster
     * @param ambari ambari server parameters
     */
    Cluster add(String id, Ambari ambari) throws ConnectionException;

    /**
     * Removes a cluster from the registry.
     *
     * @param id id of the cluster
     */
    Cluster remove(String id);

    /**
     * Retrieves the registered cluster.
     *
     * @param id id of the cluster
     * @return cluster registration or null
     */
    Cluster get(String id);

    /**
     * Returns all the registered clusters.
     *
     * @return collection of clusters
     */
    List<Cluster> getAll();
}
