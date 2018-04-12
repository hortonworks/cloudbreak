package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.ClusterComponent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Set;

@EntityType(entityClass = ClusterComponent.class)
public interface ClusterComponentRepository extends CrudRepository<ClusterComponent, Long> {

    @Query("SELECT cv FROM ClusterComponent cv WHERE cv.cluster.id = :clusterId AND cv.componentType = :componentType AND cv.name = :name")
    ClusterComponent findComponentByClusterIdComponentTypeName(@Param("clusterId") Long clusterId, @Param("componentType") ComponentType componentType,
            @Param("name") String name);

    @Query("SELECT cv FROM ClusterComponent cv WHERE cv.cluster.id = :clusterId")
    Set<ClusterComponent> findComponentByClusterId(@Param("clusterId") Long clusterId);

    Set<ClusterComponent> findByComponentType(ComponentType componentType);
}