package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class StatusTest {

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
            if (!status.isInProgress() && (status.name().contains("REQUESTED") || status.name().contains("IN_PROGRESS"))) {
                fail(status.name() + " looks like an in progress state, please put it into the in progress list");
            }
        }
    }

}