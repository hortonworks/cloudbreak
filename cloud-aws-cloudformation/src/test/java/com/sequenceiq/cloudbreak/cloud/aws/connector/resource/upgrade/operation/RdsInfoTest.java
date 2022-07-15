package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class RdsInfoTest {

    @Test
    void testCtorAndGetters() {
        RdsState rdsState = RdsState.UNKNOWN;
        Map<String, String> instancesToStatusMap = Map.of("instance", "status");
        RdsEngineVersion rdsEngineVersion = new RdsEngineVersion("engineVersion");

        RdsInfo rdsInfo = new RdsInfo(rdsState, instancesToStatusMap, rdsEngineVersion);

        assertEquals(RdsState.UNKNOWN, rdsInfo.getRdsState());
        assertEquals(rdsEngineVersion, rdsInfo.getRdsEngineVersion());
        assertEquals(instancesToStatusMap, rdsInfo.getDbArnToInstanceStatuses());
    }

}
