package com.sequenceiq.redbeams.service;

import static com.sequenceiq.redbeams.api.model.common.SimulatedDatabaseCapacityFailureTag.ALL_ATTEMPTS;
import static com.sequenceiq.redbeams.api.model.common.SimulatedDatabaseCapacityFailureTag.KEY;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.redbeams.api.model.common.SimulatedDatabaseCapacityFailureTag;

/**
 * Test-only hook that lets E2E tests exercise the automatic database instance-type fallback on real cloud providers, where a
 * capacity shortage cannot be forced. For accounts having the {@code CDP_SIMULATE_DATABASE_CAPACITY_FAILURE} entitlement, the
 * {@link SimulatedDatabaseCapacityFailureTag} tag of the database stack tells the provider how many of the leading instance-type
 * attempts to treat as a capacity failure. The tag is silently ignored for other accounts.
 */
@Service
public class SimulatedDatabaseCapacityFailureService {

    private static final int INVALID_FAILURE_COUNT = -1;

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedDatabaseCapacityFailureService.class);

    @Inject
    private EntitlementService entitlementService;

    public void applyIfRequested(String accountId, DatabaseStack databaseStack) {
        Map<String, String> tags = databaseStack.getTags();
        String tagValue = tags == null ? null : tags.get(KEY);
        if (StringUtils.isBlank(tagValue)) {
            return;
        }
        if (!entitlementService.isSimulateDatabaseCapacityFailureEnabled(accountId)) {
            LOGGER.debug("Ignoring tag '{}' as the account is not entitled to simulate database capacity failures", KEY);
            return;
        }
        parseFailureCount(tagValue).ifPresent(failureCount -> {
            LOGGER.warn("Simulating capacity failure for the first {} database instance-type attempt(s) as requested by tag '{}'", failureCount, KEY);
            databaseStack.getDatabaseServer().putParameter(DatabaseServer.SIMULATED_CAPACITY_FAILURES, failureCount);
        });
    }

    private Optional<Integer> parseFailureCount(String tagValue) {
        String value = tagValue.trim();
        if (ALL_ATTEMPTS.equalsIgnoreCase(value)) {
            return Optional.of(Integer.MAX_VALUE);
        }
        int failureCount = NumberUtils.toInt(value, INVALID_FAILURE_COUNT);
        if (failureCount >= 0) {
            return Optional.of(failureCount);
        }
        LOGGER.warn("Ignoring invalid value '{}' of tag '{}', expected a non-negative number or '{}'", value, KEY, ALL_ATTEMPTS);
        return Optional.empty();
    }
}
