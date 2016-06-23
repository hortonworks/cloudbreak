package com.sequenceiq.cloudbreak.repository

import java.util.Date

import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

import org.springframework.data.jpa.domain.Specification

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent

object CloudbreakEventSpecifications {

    fun eventsForUser(user: String): Specification<CloudbreakEvent> {
        return Specification { cloudbreakEventRoot, query, cb -> cb.equal(cloudbreakEventRoot.get<Any>("owner"), user) }
    }

    fun eventsSince(since: Long?): Specification<CloudbreakEvent> {
        return Specification { cloudbreakEventRoot, query, cb ->
            if (since != null) {
                cb.greaterThanOrEqualTo(cloudbreakEventRoot.get<Date>("eventTimestamp"), Date(since))
            } else {
                cb.and()
            }
        }
    }

}
