package com.sequenceiq.authorization.resource;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RightUtilsTest {

    @Test
    public void testGetNonDatahubRight() {
        assertEquals("environment/write", RightUtils.getRight(AuthorizationResource.ENVIRONMENT, ResourceAction.WRITE));
    }

    @Test
    public void testGetDatahubRight() {
        assertEquals("datahub/write", RightUtils.getRight(AuthorizationResource.DATAHUB, ResourceAction.WRITE));
    }
}