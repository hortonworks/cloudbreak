package com.sequenceiq.authorization.resource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ResourceActionTest {

    @Test
    public void testGetByName() {
        assertTrue(ResourceAction.getByName("write").isPresent());
        assertFalse(ResourceAction.getByName("invalid").isPresent());
    }

}
