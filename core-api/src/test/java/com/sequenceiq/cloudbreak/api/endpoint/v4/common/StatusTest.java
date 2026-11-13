package com.sequenceiq.cloudbreak.api.endpoint.v4.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void hasSameNotificationAsTest() {
        assertTrue(Status.UNREACHABLE.hasSameNotificationAs(Status.NODE_FAILURE));
        assertTrue(Status.NODE_FAILURE.hasSameNotificationAs(Status.UNREACHABLE));
        assertTrue(Status.NODE_FAILURE.hasSameNotificationAs(Status.NODE_FAILURE));
        assertFalse(Status.NODE_FAILURE.hasSameNotificationAs(Status.AVAILABLE));
        assertFalse(Status.AVAILABLE.hasSameNotificationAs(Status.NODE_FAILURE));
        assertFalse(Status.NODE_FAILURE.hasSameNotificationAs(Status.UPDATE_FAILED));
        assertFalse(Status.RESTORE_FAILED.hasSameNotificationAs(Status.BACKUP_FAILED));
        assertFalse(Status.NODE_FAILURE.hasSameNotificationAs(null));
    }

    @Test
    void hasSameNotificationAsComparesOnlyNotificationIdentity() {
        // Statuses that trigger no notification have the same (absent) notification, so the comparison is reflexive
        // and symmetric. Filtering out the no-notification case is shouldTriggerNotification()'s job, not this one's.
        assertTrue(Status.AVAILABLE.hasSameNotificationAs(Status.STOPPED));
        assertTrue(Status.AVAILABLE.hasSameNotificationAs(Status.AVAILABLE));
        assertFalse(Status.AVAILABLE.shouldTriggerNotification());
        assertFalse(Status.STOPPED.shouldTriggerNotification());
    }
}