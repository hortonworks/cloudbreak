package com.sequenceiq.cloudbreak.repository

import java.util.Date

import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

import org.springframework.data.jpa.domain.Specification

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage

object CloudbreakUsageSpecifications {

    fun usagesSince(since: Long?): Specification<CloudbreakUsage> {
        return Specification { cloudbreakUsageRoot, query, cb -> if (since == null) cb.and() else cb.greaterThanOrEqualTo(cloudbreakUsageRoot.get<Date>("day"), Date(since)) }
    }

    fun usagesWithStringFields(field: String, value: String?): Specification<CloudbreakUsage> {
        return Specification { cloudbreakUsageRoot, query, cb -> if (value == null) cb.and() else cb.equal(cloudbreakUsageRoot.get<Any>(field), value) }
    }

    fun usagesBefore(date: Long?): Specification<CloudbreakUsage> {
        return Specification { cloudbreakUsageRoot, query, cb -> if (date == null) cb.and() else cb.lessThan(cloudbreakUsageRoot.get<Date>("day"), Date(date)) }
    }
}
