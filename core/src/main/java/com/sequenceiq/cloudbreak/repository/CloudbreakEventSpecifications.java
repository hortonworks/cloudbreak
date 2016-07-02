package com.sequenceiq.cloudbreak.repository;

import java.util.Date;

import org.springframework.data.jpa.domain.Specification;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;

public class CloudbreakEventSpecifications {

    private CloudbreakEventSpecifications() {
    }

    public static Specification<CloudbreakEvent> eventsForUser(final String user) {
        return (cloudbreakEventRoot, query, cb) -> cb.equal(cloudbreakEventRoot.get("owner"), user);
    }

    public static Specification<CloudbreakEvent> eventsSince(final Long since) {
        return (cloudbreakEventRoot, query, cb) -> {
            if (since != null) {
                return cb.greaterThanOrEqualTo(cloudbreakEventRoot.get("eventTimestamp"), new Date(since));
            } else {
                return cb.and();
            }
        };
    }

}
