package com.sequenceiq.cloudbreak.repository;

import java.util.Date;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;

public class CloudbreakUsageSpecifications {
    private CloudbreakUsageSpecifications() {
    }

    public static Specification<CloudbreakUsage> usagesSince(final Long since) {
        return new Specification<CloudbreakUsage>() {
            @Override
            public Predicate toPredicate(final Root<CloudbreakUsage> cloudbreakUsageRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                return (since == null) ? cb.and() : cb.greaterThanOrEqualTo(cloudbreakUsageRoot.<Date>get("day"), new Date(since));
            }
        };
    }

    public static Specification<CloudbreakUsage> usagesWithStringFields(final String field, final String value) {
        return new Specification<CloudbreakUsage>() {
            @Override
            public Predicate toPredicate(final Root<CloudbreakUsage> cloudbreakUsageRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                return (value == null) ? cb.and() : cb.equal(cloudbreakUsageRoot.get(field), value);
            }
        };
    }

    public static Specification<CloudbreakUsage> usagesBefore(final Long date) {
        return new Specification<CloudbreakUsage>() {
            @Override
            public Predicate toPredicate(final Root<CloudbreakUsage> cloudbreakUsageRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                return (date == null) ? cb.and() : cb.lessThan(cloudbreakUsageRoot.<Date>get("day"), new Date(date));
            }
        };
    }
}
