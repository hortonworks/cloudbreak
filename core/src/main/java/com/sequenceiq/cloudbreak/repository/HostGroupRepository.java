package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;

@EntityType(entityClass = HostGroup.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface HostGroupRepository extends CrudRepository<HostGroup, Long> {

    @EntityGraph(value = "HostGroup.constraint.instanceGroup.instanceMetaData", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT h FROM HostGroup h LEFT JOIN FETCH h.hostMetadata LEFT JOIN FETCH h.recipes WHERE h.cluster.id= :clusterId")
    Set<HostGroup> findHostGroupsInCluster(@Param("clusterId") Long clusterId);

    @EntityGraph(value = "HostGroup.constraint.instanceGroup.instanceMetaData", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT h FROM HostGroup h LEFT JOIN FETCH h.hostMetadata LEFT JOIN FETCH h.recipes WHERE h.cluster.id= :clusterId AND h.name= :hostGroupName")
    HostGroup findHostGroupInClusterByName(@Param("clusterId") Long clusterId, @Param("hostGroupName") String hostGroupName);

    @EntityGraph(value = "HostGroup.constraint.instanceGroup.instanceMetaData", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT h FROM HostGroup h JOIN h.recipes r WHERE r.id= :recipeId")
    Set<HostGroup> findAllHostGroupsByRecipe(@Param("recipeId") Long recipeId);

    @EntityGraph(value = "HostGroup.constraint.instanceGroup.instanceMetaData", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT h FROM HostGroup h WHERE h.cluster.id= :clusterId AND h.constraint.instanceGroup.groupName= :instanceGroupName")
    HostGroup findHostGroupsByInstanceGroupName(@Param("clusterId") Long clusterId, @Param("instanceGroupName") String instanceGroupName);

}
