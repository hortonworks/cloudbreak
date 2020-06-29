package com.sequenceiq.cloudbreak.api.endpoint.v4.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

class StatusTest {

    @Test
    void mapToFailedIfInProgressTest() {
        assertTrue(Sets.intersection(Status.mappableToFailed(), Status.notMappableToFailed()).isEmpty());
        assertEquals(EnumSet.allOf(Status.class), Sets.union(Status.mappableToFailed(), Status.notMappableToFailed()));

        for (Status s : Status.mappableToFailed()) {
            Status mapped = s.mapToFailedIfInProgress();
            assertNotEquals(s, mapped);
        }
    }
}