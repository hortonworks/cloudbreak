package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.HostGroup

@EntityType(entityClass = HostGroup::class)
interface HostGroupRepository : CrudRepository<HostGroup, Long> {

    fun findHostGroupsInCluster(@Param("clusterId") clusterId: Long?): Set<HostGroup>

    fun findHostGroupInClusterByName(@Param("clusterId") clusterId: Long?, @Param("hostGroupName") hostGroupName: String): HostGroup

    fun findAllHostGroupsByRecipe(@Param("recipeId") recipeId: Long?): Set<HostGroup>

    fun findHostGroupsByInstanceGroupName(@Param("clusterId") clusterId: Long?, @Param("instanceGroupName") instanceGroupName: String): HostGroup

}
