package com.sequenceiq.redbeams.api.endpoint.v4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class ResourceStatusTest {

    @Test
    void testReleasability() {
        Set<ResourceStatus> releasableValues = ResourceStatus.getReleasableValues();
        assertEquals(1, releasableValues.size());
        assertTrue(releasableValues.contains(ResourceStatus.SERVICE_MANAGED));

        assertTrue(ResourceStatus.SERVICE_MANAGED.isReleasable());
        assertFalse(ResourceStatus.USER_MANAGED.isReleasable());
    }

}
