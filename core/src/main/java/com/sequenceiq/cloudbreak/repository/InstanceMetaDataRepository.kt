package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.InstanceMetaData

@EntityType(entityClass = InstanceMetaData::class)
interface InstanceMetaDataRepository : CrudRepository<InstanceMetaData, Long> {

    fun findNotTerminatedForStack(@Param("stackId") stackId: Long?): Set<InstanceMetaData>

    fun findAllInStack(@Param("stackId") stackId: Long?): Set<InstanceMetaData>

    fun findByInstanceId(@Param("stackId") stackId: Long?, @Param("instanceId") instanceId: String): InstanceMetaData

    fun findHostInStack(@Param("stackId") stackId: Long?, @Param("hostName") hostName: String): InstanceMetaData

    fun findUnregisteredHostsInInstanceGroup(@Param("instanceGroupId") instanceGroupId: Long?): Set<InstanceMetaData>

    fun findUnusedHostsInInstanceGroup(@Param("instanceGroupId") instanceGroupId: Long?): Set<InstanceMetaData>

    fun findAliveInstancesHostNamesInInstanceGroup(@Param("instanceGroupId") instanceGroupId: Long?): List<String>

    fun findAliveInstancesInInstanceGroup(@Param("instanceGroupId") instanceGroupId: Long?): List<InstanceMetaData>

    fun findRemovableInstances(@Param("stackId") stackId: Long?, @Param("groupName") groupName: String): Set<InstanceMetaData>

    fun findNotTerminatedByPrivateAddress(@Param("stackId") stackId: Long?, @Param("privateAddress") privateAddress: String): InstanceMetaData

}
