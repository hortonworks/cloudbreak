package com.sequenceiq.authorization.resource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AuthorizationResourceTest {

    @Test
    public void testGetByName() {
        assertTrue(AuthorizationResource.getByName("datalake").isPresent());
        assertFalse(AuthorizationResource.getByName("invalid").isPresent());
    }
}
