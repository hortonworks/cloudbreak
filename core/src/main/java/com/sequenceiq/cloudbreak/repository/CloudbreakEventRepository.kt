package com.sequenceiq.cloudbreak.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent

@EntityType(entityClass = CloudbreakEvent::class)
interface CloudbreakEventRepository : PagingAndSortingRepository<CloudbreakEvent, Long>, JpaSpecificationExecutor<Any> {

    fun findCloudbreakEventsForStack(@Param("stackId") stackId: Long?): List<CloudbreakEvent>

}
