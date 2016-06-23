package com.sequenceiq.cloudbreak.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage

@EntityType(entityClass = CloudbreakUsage::class)
interface CloudbreakUsageRepository : CrudRepository<CloudbreakUsage, Long>, JpaSpecificationExecutor<Any>
