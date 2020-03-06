package com.sequenceiq.authorization.resource;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RightUtilsTest {

    @Test
    public void testGetNonDatahubRight() {
        assertEquals("environments/write", RightUtils.getResourceDependentRight(AuthorizationResourceType.ENVIRONMENT, AuthorizationResourceAction.WRITE));
    }

    @Test
    public void testGetDatahubRight() {
        assertEquals("datahub/write", RightUtils.getResourceDependentRight(AuthorizationResourceType.DATAHUB, AuthorizationResourceAction.WRITE));
    }
}