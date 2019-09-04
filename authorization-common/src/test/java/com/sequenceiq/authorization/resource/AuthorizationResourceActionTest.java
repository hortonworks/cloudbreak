package com.sequenceiq.authorization.resource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AuthorizationResourceActionTest {

    @Test
    public void testGetByName() {
        assertTrue(AuthorizationResourceAction.getByName("write").isPresent());
        assertFalse(AuthorizationResourceAction.getByName("invalid").isPresent());
    }

}
