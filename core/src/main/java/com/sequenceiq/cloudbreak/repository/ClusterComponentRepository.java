package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.ClusterComponent;

@EntityType(entityClass = ClusterComponent.class)
public interface ClusterComponentRepository extends CrudRepository<ClusterComponent, Long> {

    ClusterComponent findComponentByClusterIdComponentTypeName(@Param("clusterId") Long clusterId, @Param("componentType") ComponentType componentType,
            @Param("name") String name);

    Set<ClusterComponent> findComponentByClusterId(@Param("clusterId") Long clusterId);
}