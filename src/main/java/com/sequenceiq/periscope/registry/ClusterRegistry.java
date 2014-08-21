package com.sequenceiq.periscope.registry;

import java.util.List;

import com.sequenceiq.periscope.model.Cluster;
import com.sequenceiq.periscope.model.ClusterDetails;

public interface ClusterRegistry {

    /**
     * Adds a new hadoop cluster to the registry.
     *
     * @param clusterDetails details of the cluster
     */
    Cluster add(ClusterDetails clusterDetails) throws ConnectionException;

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
