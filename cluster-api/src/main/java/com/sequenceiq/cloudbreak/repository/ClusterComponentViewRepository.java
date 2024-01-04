package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = ClusterComponentView.class)
@Transactional(TxType.REQUIRED)
public interface ClusterComponentViewRepository extends CrudRepository<ClusterComponentView, Long> {
    ClusterComponentView findOneByClusterIdAndComponentTypeAndName(@Param("clusterId") Long clusterId, @Param("componentType") ComponentType componentType,
            @Param("name") String name);

    @Query("SELECT cc FROM ClusterComponentView cc WHERE cc.clusterId = :clusterId AND cc.componentType = :componentType")
    Set<ClusterComponentView> findComponentViewsByClusterIdAndComponentType(
            @Param("clusterId") Long clusterId,
            @Param("componentType") ComponentType componentType);

    @Query("SELECT cc FROM ClusterComponentView cc WHERE cc.clusterId = :clusterId")
    Set<ClusterComponentView> findComponentViewByClusterId(@Param("clusterId") Long clusterId);

    @Query("SELECT cc FROM ClusterComponentView cc WHERE cc.clusterId = :clusterId AND cc.componentType in :types")
    Set<ClusterComponentView> findComponentsByClusterIdAndInComponentType(@Param("clusterId") Long clusterId, @Param("types") Collection<ComponentType> types);
}
