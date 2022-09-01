package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AwsRdsStateTest {

    @Test
    void testEnumValues() {
        assertEquals("available", AwsRdsState.AVAILABLE.getState());
        assertEquals("upgrading", AwsRdsState.UPGRADING.getState());
    }

}