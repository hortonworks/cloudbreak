package com.sequenceiq.authorization.resource;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RightUtilsTest {

    @Test
    public void testGetNonDatahubRight() {
        assertEquals("environments/write", RightUtils.getRight(ResourceType.ENVIRONMENT, ResourceAction.WRITE));
    }

    @Test
    public void testGetDatahubRight() {
        assertEquals("datahub/write", RightUtils.getRight(ResourceType.DATAHUB, ResourceAction.WRITE));
    }
}