package com.sequenceiq.redbeams.api.endpoint.v4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

public class ResourceStatusTest {

    @Test
    public void testReleasability() {
        Set<ResourceStatus> releasableValues = ResourceStatus.getReleasableValues();
        assertEquals(1, releasableValues.size());
        assertTrue(releasableValues.contains(ResourceStatus.SERVICE_MANAGED));

        assertTrue(ResourceStatus.SERVICE_MANAGED.isReleasable());
        assertFalse(ResourceStatus.USER_MANAGED.isReleasable());
    }

}
