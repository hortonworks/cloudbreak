package com.sequenceiq.periscope.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;

@EntityType(entityClass = Cluster.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ClusterRepository extends CrudRepository<Cluster, Long> {

    Cluster findByStackId(@Param("stackId") Long stackId);

    @Query(" SELECT c FROM Cluster c LEFT JOIN FETCH c.clusterPertain " +
            " WHERE c.stackCrn = :stackCrn and c.clusterPertain.tenant = :tenant")
    Optional<Cluster> findByStackCrnAndTenant(@Param("stackCrn") String stackCrn, @Param("tenant") String tenant);

    @Query(" SELECT c FROM Cluster c LEFT JOIN FETCH c.clusterPertain " +
            " WHERE c.stackName = :stackName and c.clusterPertain.tenant = :tenant")
    Optional<Cluster> findByStackNameAndTenant(@Param("stackName") String stackName, @Param("tenant") String tenant);

    @Query("SELECT c.stackCrn FROM Cluster c WHERE c.id = :id")
    String findStackCrnById(@Param("id") Long id);

    @Query("SELECT c.id FROM Cluster c WHERE c.stackCrn = :stackCrn")
    Long findIdStackCrn(@Param("stackCrn") String stackCrn);

    @Query("SELECT c FROM Cluster c WHERE c.id IN :clusterIds")
    List<Cluster> findClustersByClusterIds(@Param("clusterIds") List<Long> clusterIds);

    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.clusterPertain WHERE c.clusterPertain.tenant = :tenant and c.stackType = :stackType")
    List<Cluster> findByTenantAndStackType(@Param("tenant") String tenant, @Param("stackType") StackType stackType);

    List<Cluster> findByEnvironmentCrnOrMachineUserCrn(String environmentCrn, String machineUserCrn);

    @Query("SELECT distinct c.id FROM Cluster c JOIN c.loadAlerts loadalert WHERE c.stackType = :stackType " +
            " and c.autoscalingEnabled = :autoScalingEnabled" +
            " and c.state = :clusterState  " +
            " and (:periscopeNodeId IS NULL or c.periscopeNodeId = :periscopeNodeId) ")
    List<Long> findByLoadAlertAndStackTypeAndClusterStateAndAutoscaling(@Param("stackType") StackType stackType,
            @Param("clusterState") ClusterState clusterState,
            @Param("autoScalingEnabled") Boolean autoScalingEnabled,
            @Param("periscopeNodeId") String periscopeNodeId);

    @Query("SELECT distinct c.id FROM Cluster c JOIN c.timeAlerts timealert WHERE c.stackType = :stackType " +
            " and c.autoscalingEnabled = :autoScalingEnabled" +
            " and (:periscopeNodeId IS NULL or c.periscopeNodeId = :periscopeNodeId) ")
    List<Long> findByTimeAlertAndStackTypeAndAutoscaling(@Param("stackType") StackType stackType,
            @Param("autoScalingEnabled") Boolean autoScalingEnabled,
            @Param("periscopeNodeId") String periscopeNodeId);

    @Query("SELECT distinct c.id FROM Cluster c WHERE c.stackType = :stackType " +
            " and (:periscopeNodeId IS NULL or c.periscopeNodeId = :periscopeNodeId) ")
    List<Long> findClusterIdsByStackTypeAndPeriscopeNodeId(@Param("stackType") StackType stackType,
            @Param("periscopeNodeId") String periscopeNodeId);

    @Query("SELECT distinct c.id FROM Cluster c WHERE c.stackType = :stackType " +
            " and (:periscopeNodeId IS NULL or c.periscopeNodeId = :periscopeNodeId) " +
            " and c.autoscalingEnabled = :autoscalingEnabled")
    List<Long> findClusterIdsByStackTypeAndPeriscopeNodeIdAndAutoscalingEnabled(@Param("stackType") StackType stackType,
            @Param("periscopeNodeId") String periscopeNodeId, @Param("autoscalingEnabled") Boolean autoscalingEnabled);

    List<Cluster> findByStateAndPeriscopeNodeId(ClusterState state, String nodeId);

    List<Cluster> findAllByPeriscopeNodeId(String nodeId);

    List<Cluster> findByStateAndAutoscalingEnabledAndPeriscopeNodeId(ClusterState state, boolean autoscalingEnabled, String nodeId);

    int countByStateAndAutoscalingEnabledAndPeriscopeNodeId(ClusterState state, boolean autoscalingEnabled, String nodeId);

    int countByEnvironmentCrn(String environmentCrn);

    List<Cluster> findAllByPeriscopeNodeIdNotInOrPeriscopeNodeIdIsNull(List<String> nodes);

    @Modifying
    @Query("UPDATE Cluster c SET c.periscopeNodeId = NULL WHERE c.periscopeNodeId = :periscopeNodeId")
    void deallocateClustersOfNode(@Param("periscopeNodeId") String periscopeNodeId);

    @Modifying
    @Query("UPDATE Cluster c SET c.lastEvaluated = :lastEvaluated WHERE c.id = :clusterId")
    void setClusterLastEvaluated(@Param("clusterId") Long clusterId, @Param("lastEvaluated") Long lastEvaluated);

    @Modifying
    @Query("UPDATE Cluster c SET c.environmentCrn = :environmentCrn WHERE c.id = :clusterId")
    void setEnvironmentCrn(@Param("clusterId") Long clusterId, @Param("environmentCrn") String environmentCrn);

    @Modifying
    @Query("UPDATE Cluster c SET c.machineUserCrn = :machineUserCrn WHERE c.id = :clusterId")
    void setMachineUserCrn(@Param("clusterId") Long clusterId, @Param("machineUserCrn") String machineUserCrn);

    @Modifying
    @Query("UPDATE Cluster c SET c.lastScalingActivity = :lastScalingActivity WHERE c.id = :clusterId")
    void setClusterLastScalingActivity(@Param("clusterId") Long clusterId, @Param("lastScalingActivity") Long lastScalingActivity);
}
