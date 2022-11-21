package com.sequenceiq.cloudbreak.cloud.model.filesystem.efs;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ThroughputModeTest {
    @Test
    public void testFromValueEmptyInput() {
        String throughputValue = "";

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ThroughputMode.fromValue(throughputValue);
        });

        String expectedMessage = "Value cannot be null or empty!";
        exception.getMessage().equals(expectedMessage);
    }

    @Test
    public void testFromValueValidInput() {
        String throughputValue = "BURSTING";

        ThroughputMode throughputMode = ThroughputMode.fromValue(throughputValue);
        throughputMode.equals(ThroughputMode.BURSTING);
    }
}
