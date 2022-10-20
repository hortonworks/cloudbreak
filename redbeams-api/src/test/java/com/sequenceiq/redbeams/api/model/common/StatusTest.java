package com.sequenceiq.redbeams.api.model.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class StatusTest {

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"DELETE_IN_PROGRESS", "PRE_DELETE_IN_PROGRESS", "DELETE_FAILED", "DELETE_REQUESTED"})
    public void testIsDeleteInProgressOrFailed(Status deleteStatus) {
        assertTrue(deleteStatus.isDeleteInProgressOrFailed());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"DELETE_IN_PROGRESS", "PRE_DELETE_IN_PROGRESS", "DELETE_COMPLETED", "DELETE_REQUESTED"})
    public void testIsDeleteInProgressOrCompleted(Status deleteStatus) {
        assertTrue(deleteStatus.isDeleteInProgressOrCompleted());
    }

    @Test
    public void testIsSuccessfullyDeleted() {
        assertTrue(Status.DELETE_COMPLETED.isSuccessfullyDeleted());
    }

    @Test
    public void testGetDeletingStatuses() {
        Set<Status> deletingStatuses = Status.getDeletingStatuses();

        assertThat(deletingStatuses, hasItem(Status.PRE_DELETE_IN_PROGRESS));
        assertThat(deletingStatuses, hasItem(Status.DELETE_REQUESTED));
        assertThat(deletingStatuses, hasItem(Status.DELETE_IN_PROGRESS));
        assertThat(deletingStatuses, hasItem(Status.DELETE_FAILED));
        assertThat(deletingStatuses, hasItem(Status.DELETE_COMPLETED));
    }

    @Test
    public void mapToFailedIfInProgressTest() {
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
    public void inProgressTest() {
        for (Status status : Status.values()) {
            if (status.name().contains("REQUESTED") || status.name().contains("IN_PROGRESS")) {
                if (!status.isInProgress()) {
                    fail(status.name() + " looks like an in progress state, please put it into the in progress list");
                }
            }
        }
    }
}
