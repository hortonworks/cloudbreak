package com.sequenceiq.cloudbreak.repository;

import java.util.Date;

import org.springframework.data.jpa.domain.Specification;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;

public class CloudbreakUsageSpecifications {
    private CloudbreakUsageSpecifications() {
    }

    public static Specification<CloudbreakUsage> usagesSince(final Long since) {
        return (cloudbreakUsageRoot, query, cb) -> (since == null) ? cb.and() : cb.greaterThanOrEqualTo(cloudbreakUsageRoot.get("day"), new Date(since));
    }

    public static Specification<CloudbreakUsage> usagesWithStringFields(final String field, final String value) {
        return (cloudbreakUsageRoot, query, cb) -> (value == null) ? cb.and() : cb.equal(cloudbreakUsageRoot.get(field), value);
    }

    public static Specification<CloudbreakUsage> usagesBefore(final Long date) {
        return (cloudbreakUsageRoot, query, cb) -> (date == null) ? cb.and() : cb.lessThan(cloudbreakUsageRoot.get("day"), new Date(date));
    }
}
