package com.sequenceiq.periscope.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.BaseRepository;
import com.sequenceiq.cloudbreak.aspect.DisablePermission;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;

@HasPermission
@EntityType(entityClass = Cluster.class)
public interface ClusterRepository extends BaseRepository<Cluster, Long> {

    Cluster findByStackId(@Param("stackId") Long stackId);

    List<Cluster> findByUserId(String id);

    List<Cluster> findByStateAndPeriscopeNodeId(ClusterState state, String nodeId);

    List<Cluster> findByStateAndAutoscalingEnabledAndPeriscopeNodeId(ClusterState state, boolean autoscalingEnabled, String nodeId);

    @DisablePermission
    int countByStateAndAutoscalingEnabledAndPeriscopeNodeId(ClusterState state, boolean autoscalingEnabled, String nodeId);

    List<Cluster> findAllByPeriscopeNodeIdNotInOrPeriscopeNodeIdIsNull(List<String> nodes);

    @Modifying
    @Query("UPDATE Cluster c SET c.periscopeNodeId = :periscopeNodeId WHERE c.id = :id")
    void allocateClusterForNode(@Param("id") long id, @Param("periscopeNodeId") String periscopeNodeId);

    @Modifying
    @Query("UPDATE Cluster c SET c.periscopeNodeId = NULL WHERE c.periscopeNodeId = :periscopeNodeId")
    void deallocateClustersOfNode(@Param("periscopeNodeId") String periscopeNodeId);
}
