package com.sequenceiq.authorization.resource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AuthorizationResourceTypeTest {

    @Test
    public void testGetByName() {
        assertTrue(AuthorizationResourceType.getByName("datalake").isPresent());
        assertFalse(AuthorizationResourceType.getByName("invalid").isPresent());
    }
}
