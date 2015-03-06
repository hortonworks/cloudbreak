package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.HostGroup;

public interface HostGroupRepository extends CrudRepository<HostGroup, Long> {

    Set<HostGroup> findHostGroupsInCluster(@Param("clusterId") Long clusterId);

    HostGroup findHostGroupInClusterByName(@Param("clusterId") Long clusterId, @Param("hostGroupName") String hostGroupName);

    Set<HostGroup> findAllHostGroupsByRecipe(@Param("recipeId") Long recipeId);

}
