package com.sequenceiq.cloudbreak.cloud.model.filesystem.efs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class LifeCycleStateTest {
    @Test
    public void testFromValue() {
        String responseValue = "creating";

        LifeCycleState responseState = LifeCycleState.fromValue(responseValue);
        assertTrue(LifeCycleState.CREATING.equals(responseState));
    }

    @Test
    public void testToStringLowerCase() {
        String responseValue = LifeCycleState.CREATING.toString();

        // make sure the returned value is exactly what the cloud provider expects
        assertEquals("creating", responseValue);
    }
}
