package com.sequenceiq.periscope.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;

@EntityType(entityClass = Cluster.class)
public interface ClusterRepository extends CrudRepository<Cluster, Long> {

    Cluster findByStackId(@Param("stackId") Long stackId);

    @Query(" SELECT c FROM Cluster c LEFT JOIN FETCH c.clusterPertain " +
            " WHERE c.stackCrn = :stackCrn and c.clusterPertain.userId = :userId")
    Optional<Cluster> findByStackCrnAndUserId(@Param("stackCrn") String stackCrn, @Param("userId") String userId);

    @Query(" SELECT c FROM Cluster c LEFT JOIN FETCH c.clusterPertain " +
            " WHERE c.stackName = :stackName and c.clusterPertain.userId = :userId")
    Optional<Cluster> findByStackNameAndUserId(@Param("stackName") String stackName, @Param("userId") String userId);

    @Query("SELECT c.stackCrn FROM Cluster c WHERE c.id = :id")
    String findStackCrnById(@Param("id") Long id);

    @Query("SELECT c.id FROM Cluster c WHERE c.stackCrn = :stackCrn")
    Long findIdStackCrn(@Param("stackCrn") String stackCrn);

    @Query("SELECT c FROM Cluster c WHERE c.id IN :clusterIds")
    List<Cluster> findClustersByClusterIds(@Param("clusterIds") List<Long> clusterIds);

    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.clusterPertain WHERE c.clusterPertain.userId = :userId")
    List<Cluster> findByUserId(@Param("userId") String userId);

    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.clusterPertain WHERE c.clusterPertain.userId = :userId and c.stackType = :stackType")
    List<Cluster> findByUserIdAndStackType(@Param("userId") String userId, @Param("stackType") StackType stackType);

    @Query("SELECT distinct c.id FROM Cluster c JOIN c.loadAlerts loadalert WHERE c.stackType = :stackType " +
            " and c.autoscalingEnabled = :autoScalingEnabled" +
            " and c.state = :clusterState  " +
            " and (:periscopeNodeId IS NULL or c.periscopeNodeId = :periscopeNodeId) ")
    List<Long> findByLoadAlertAndStackTypeAndClusterStateAndAutoscaling(@Param("stackType") StackType stackType,
            @Param("clusterState") ClusterState clusterState,
            @Param("autoScalingEnabled") Boolean autoScalingEnabled,
            @Param("periscopeNodeId") String periscopeNodeId);

    List<Cluster> findByStateAndPeriscopeNodeId(ClusterState state, String nodeId);

    List<Cluster> findByStateAndAutoscalingEnabledAndPeriscopeNodeId(ClusterState state, boolean autoscalingEnabled, String nodeId);

    int countByStateAndAutoscalingEnabledAndPeriscopeNodeId(ClusterState state, boolean autoscalingEnabled, String nodeId);

    List<Cluster> findAllByPeriscopeNodeIdNotInOrPeriscopeNodeIdIsNull(List<String> nodes);

    @Modifying
    @Query("UPDATE Cluster c SET c.periscopeNodeId = NULL WHERE c.periscopeNodeId = :periscopeNodeId")
    void deallocateClustersOfNode(@Param("periscopeNodeId") String periscopeNodeId);
}
