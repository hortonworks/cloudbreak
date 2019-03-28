package com.sequenceiq.cloudbreak.authorization;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WorkspaceRightUtilsTest {

    @Test
    public void testGetRight() {
        assertEquals("distrox/writeDatabase", WorkspaceRightUtils.getRight(WorkspaceResource.DATABASE, ResourceAction.WRITE));
    }
}
