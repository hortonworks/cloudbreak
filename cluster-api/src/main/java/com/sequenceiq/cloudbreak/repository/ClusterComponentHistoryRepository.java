package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent_History;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = ClusterComponent_History.class)
@Transactional(TxType.REQUIRED)
public interface ClusterComponentHistoryRepository extends CrudRepository<ClusterComponent_History, Long> {

    @Query("SELECT cv FROM ClusterComponent_History cv WHERE cv.cluster.id = :clusterId AND cv.componentType = :componentType AND cv.name = :name")
    ClusterComponent_History findComponentByClusterIdComponentTypeName(@Param("clusterId") Long clusterId, @Param("componentType") ComponentType componentType,
            @Param("name") String name);

    @Query("SELECT cv FROM ClusterComponent_History cv WHERE cv.cluster.id = :clusterId AND cv.componentType = :componentType AND cv.name = :name")
    Set<ClusterComponent_History> findComponentsByClusterIdComponentTypeName(@Param("clusterId") Long clusterId,
            @Param("componentType") ComponentType componentType, @Param("name") String name);

    @Query("SELECT cv FROM ClusterComponent_History cv WHERE cv.cluster.id = :clusterId AND cv.componentType = :componentType")
    Set<ClusterComponent_History> findComponentsByClusterIdAndComponentType(@Param("clusterId") Long clusterId,
            @Param("componentType") ComponentType componentType);

    @Query("SELECT cv FROM ClusterComponent_History cv WHERE cv.cluster.id = :clusterId")
    Set<ClusterComponent_History> findComponentByClusterId(@Param("clusterId") Long clusterId);

    @EntityGraph(value = "ClusterComponentHistory.cluster.rdsConfig", type = EntityGraphType.LOAD)
    Set<ClusterComponent_History> findByComponentType(ComponentType componentType);

}