package com.sequenceiq.periscope.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;

@EntityType(entityClass = Cluster.class)
public interface ClusterRepository extends CrudRepository<Cluster, Long> {

    Cluster findByStackId(@Param("stackId") Long stackId);

    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.clusterPertain WHERE c.clusterPertain.userId = :userId")
    List<Cluster> findByUserId(@Param("userId") String userId);

    List<Cluster> findByStateAndPeriscopeNodeId(ClusterState state, String nodeId);

    List<Cluster> findByStateAndAutoscalingEnabledAndPeriscopeNodeId(ClusterState state, boolean autoscalingEnabled, String nodeId);

    int countByStateAndAutoscalingEnabledAndPeriscopeNodeId(ClusterState state, boolean autoscalingEnabled, String nodeId);

    List<Cluster> findAllByPeriscopeNodeIdNotInOrPeriscopeNodeIdIsNull(List<String> nodes);

    @Modifying
    @Query("UPDATE Cluster c SET c.periscopeNodeId = :periscopeNodeId WHERE c.id = :id")
    void allocateClusterForNode(@Param("id") long id, @Param("periscopeNodeId") String periscopeNodeId);

    @Modifying
    @Query("UPDATE Cluster c SET c.periscopeNodeId = NULL WHERE c.periscopeNodeId = :periscopeNodeId")
    void deallocateClustersOfNode(@Param("periscopeNodeId") String periscopeNodeId);

}
