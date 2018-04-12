package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.HostGroup;

@EntityType(entityClass = HostGroup.class)
public interface HostGroupRepository extends CrudRepository<HostGroup, Long> {

    @Query("SELECT h FROM HostGroup h LEFT JOIN FETCH h.hostMetadata LEFT JOIN FETCH h.recipes WHERE h.cluster.id= :clusterId")
    Set<HostGroup> findHostGroupsInCluster(@Param("clusterId") Long clusterId);

    @Query("SELECT h FROM HostGroup h LEFT JOIN FETCH h.hostMetadata LEFT JOIN FETCH h.recipes WHERE h.cluster.id= :clusterId AND h.name= :hostGroupName")
    HostGroup findHostGroupInClusterByName(@Param("clusterId") Long clusterId, @Param("hostGroupName") String hostGroupName);

    @Query("SELECT h FROM HostGroup h JOIN h.recipes r WHERE r.id= :recipeId")
    Set<HostGroup> findAllHostGroupsByRecipe(@Param("recipeId") Long recipeId);

    @Query("SELECT h FROM HostGroup h WHERE h.cluster.id= :clusterId AND h.constraint.instanceGroup.groupName= :instanceGroupName")
    HostGroup findHostGroupsByInstanceGroupName(@Param("clusterId") Long clusterId, @Param("instanceGroupName") String instanceGroupName);

}
