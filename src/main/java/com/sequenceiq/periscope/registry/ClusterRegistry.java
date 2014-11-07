package com.sequenceiq.periscope.registry;

import java.util.List;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeUser;

public interface ClusterRegistry {

    /**
     * Adds a new hadoop cluster to the registry.
     */
    Cluster add(PeriscopeUser user, Cluster cluster);

    /**
     * Removes a cluster from the registry.
     *
     * @param id id of the cluster
     */
    Cluster remove(PeriscopeUser user, long id);

    /**
     * Retrieves the registered cluster.
     *
     * @param id id of the cluster
     * @return cluster registration or null
     */
    Cluster get(PeriscopeUser user, long id);

    /**
     * Retrieves the registered cluster, regardless of the user.
     * It is only intended to used by internal monitoring processes
     * not by the API. It is not recommended to use this method by
     * third parties.
     *
     * @param id id of the cluster
     * @return cluster registration or null
     */
    Cluster get(long id);

    /**
     * Returns all the registered clusters of a user.
     *
     * @return collection of clusters
     */
    List<Cluster> getAll(PeriscopeUser user);

    /**
     * Returns all the registered clusters.
     *
     * @return collection of clusters
     */
    List<Cluster> getAll();
}
