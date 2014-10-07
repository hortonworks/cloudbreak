package com.sequenceiq.cloudbreak.repository;

import java.util.Date;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;

public class CloudbreakEventSpecifications {

    private CloudbreakEventSpecifications() {
    }

    public static Specification<CloudbreakEvent> eventsForUser(final String user) {
        return new Specification<CloudbreakEvent>() {
            @Override
            public Predicate toPredicate(final Root<CloudbreakEvent> cloudbreakEventRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                return cb.equal(cloudbreakEventRoot.get("userId"), user);
            }
        };
    }

    public static Specification<CloudbreakEvent> eventsSince(final Long since) {
        return new Specification<CloudbreakEvent>() {
            @Override
            public Predicate toPredicate(final Root<CloudbreakEvent> cloudbreakEventRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                if (since != null) {
                    return cb.greaterThanOrEqualTo(cloudbreakEventRoot.<Date>get("eventTimestamp"), new Date(since));
                } else {
                    return cb.and();
                }
            }
        };
    }

}
