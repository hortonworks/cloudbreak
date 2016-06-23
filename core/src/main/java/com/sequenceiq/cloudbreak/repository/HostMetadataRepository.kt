package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.HostMetadata

@EntityType(entityClass = HostMetadata::class)
interface HostMetadataRepository : CrudRepository<HostMetadata, Long> {

    fun findHostsInCluster(@Param("clusterId") clusterId: Long?): Set<HostMetadata>

    fun findEmptyContainerHostsInHostGroup(@Param("hostGroupId") hostGroupId: Long?): Set<HostMetadata>

    fun findHostInClusterByName(@Param("clusterId") clusterId: Long?, @Param("hostName") hostName: String): HostMetadata
}
