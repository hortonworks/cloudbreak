package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = ClusterComponent.class)
@Transactional(TxType.REQUIRED)
public interface ClusterComponentRepository extends CrudRepository<ClusterComponent, Long> {

    @Query("SELECT cv FROM ClusterComponent cv WHERE cv.cluster.id = :clusterId AND cv.componentType = :componentType AND cv.name = :name")
    ClusterComponent findComponentByClusterIdComponentTypeName(@Param("clusterId") Long clusterId, @Param("componentType") ComponentType componentType,
            @Param("name") String name);

    @Query("SELECT cv FROM ClusterComponent cv WHERE cv.cluster.id = :clusterId")
    Set<ClusterComponent> findComponentByClusterId(@Param("clusterId") Long clusterId);

    @Modifying
    @Query("DELETE FROM ClusterComponent cv WHERE cv.cluster.id = :clusterId AND cv.componentType = :componentType")
    void deleteComponentByClusterIdAndComponentType(@Param("clusterId") Long clusterId, @Param("componentType") ComponentType componentType);

}
