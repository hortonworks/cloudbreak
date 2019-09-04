package com.sequenceiq.authorization.resource;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RightUtilsTest {

    @Test
    public void testGetNonDatahubRight() {
        assertEquals("environments/write", RightUtils.getRight(AuthorizationResourceType.ENVIRONMENT, AuthorizationResourceAction.WRITE));
    }

    @Test
    public void testGetDatahubRight() {
        assertEquals("datahub/write", RightUtils.getRight(AuthorizationResourceType.DATAHUB, AuthorizationResourceAction.WRITE));
    }
}