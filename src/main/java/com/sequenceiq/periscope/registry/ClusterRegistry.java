package com.sequenceiq.periscope.registry;

import java.util.List;

import com.sequenceiq.periscope.domain.Cluster;

public interface ClusterRegistry {

    /**
     * Adds a new hadoop cluster to the registry.
     */
    Cluster add(Cluster cluster) throws ConnectionException;

    /**
     * Removes a cluster from the registry.
     *
     * @param id id of the cluster
     */
    Cluster remove(long id);

    /**
     * Retrieves the registered cluster.
     *
     * @param id id of the cluster
     * @return cluster registration or null
     */
    Cluster get(long id);

    /**
     * Returns all the registered clusters.
     *
     * @return collection of clusters
     */
    List<Cluster> getAll();
}
