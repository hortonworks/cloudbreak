package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;

@EntityType(entityClass = ClusterComponent.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ClusterComponentRepository extends CrudRepository<ClusterComponent, Long> {

    @Query("SELECT cv FROM ClusterComponent cv WHERE cv.cluster.id = :clusterId AND cv.componentType = :componentType AND cv.name = :name")
    ClusterComponent findComponentByClusterIdComponentTypeName(@Param("clusterId") Long clusterId, @Param("componentType") ComponentType componentType,
            @Param("name") String name);

    @Query("SELECT cv FROM ClusterComponent cv WHERE cv.cluster.id = :clusterId")
    Set<ClusterComponent> findComponentByClusterId(@Param("clusterId") Long clusterId);

    @EntityGraph(value = "ClusterComponent.cluster.rdsConfig", type = EntityGraphType.LOAD)
    Set<ClusterComponent> findByComponentType(ComponentType componentType);
}