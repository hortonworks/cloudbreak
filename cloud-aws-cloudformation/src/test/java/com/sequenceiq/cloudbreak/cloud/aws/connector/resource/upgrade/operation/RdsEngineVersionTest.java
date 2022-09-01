package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class RdsEngineVersionTest {

    @Test
    void testGetters() {
        RdsEngineVersion rdsEngineVersion = new RdsEngineVersion("major.minor");

        assertEquals("major", rdsEngineVersion.getMajorVersion());
        assertEquals("major.minor", rdsEngineVersion.getVersion());
    }
}
