package com.sequenceiq.authorization.resource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ResourceTypeTest {

    @Test
    public void testGetByName() {
        assertTrue(ResourceType.getByName("datalake").isPresent());
        assertFalse(ResourceType.getByName("invalid").isPresent());
    }
}
