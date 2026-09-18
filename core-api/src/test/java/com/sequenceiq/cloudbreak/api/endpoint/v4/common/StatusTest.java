package com.sequenceiq.cloudbreak.api.endpoint.v4.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class StatusTest {

    @Test
    void mapToFailedIfInProgressTest() {
        for (Status status : Status.values()) {
            Status result = status.mapToFailedIfInProgress();
            if (status.isInProgress()) {
                assertFalse(result.isInProgress(), status + " status was mapped to " + result + " which still has PROGRESS status kind.");
            } else {
                assertEquals(status, result);
            }
        }
    }

    @Test
    void isEncryptionProfileDisableAllowedTest() {
        Set<Status> allowedStatuses = EnumSet.of(Status.AVAILABLE, Status.UPDATE_FAILED);
        for (Status status : Status.values()) {
            assertEquals(allowedStatuses.contains(status), status.isEncryptionProfileDisableAllowed(), status.name());
        }
        assertEquals(allowedStatuses, Status.getEncryptionProfileDisableAllowedStatuses());
    }
}