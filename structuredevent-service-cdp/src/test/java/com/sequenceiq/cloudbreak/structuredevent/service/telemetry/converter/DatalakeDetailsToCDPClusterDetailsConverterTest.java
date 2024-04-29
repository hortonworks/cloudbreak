package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterDetails;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.structuredevent.event.DatabaseDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.datalake.DatalakeDetails;

class DatalakeDetailsToCDPClusterDetailsConverterTest {

    private static final String AVAILABILITY_TYPE = "HA";

    private static final String ENGINE_VERSION = "engineVersion";

    private static final String ATTRIBUTES = "attributes";

    private DatalakeDetailsToCDPClusterDetailsConverter underTest = new DatalakeDetailsToCDPClusterDetailsConverter();

    @Test
    void testConvert() {
        DatalakeDetails datalakeDetails = new DatalakeDetails();
        datalakeDetails.setMultiAzEnabled(true);
        DatabaseDetails databaseDetails = new DatabaseDetails();
        datalakeDetails.setDatabaseDetails(databaseDetails);
        databaseDetails.setAvailabilityType(AVAILABILITY_TYPE);
        databaseDetails.setEngineVersion(ENGINE_VERSION);
        databaseDetails.setAttributes(ATTRIBUTES);
        CDPClusterDetails result = underTest.convert(datalakeDetails);

        assertNotNull(result.getDatabaseDetails());
        assertEquals(AVAILABILITY_TYPE, result.getDatabaseDetails().getAvailabilityType());
        assertEquals(ATTRIBUTES, result.getDatabaseDetails().getAttributes());
        assertEquals(ENGINE_VERSION, result.getDatabaseDetails().getEngineVersion());
        assertTrue(result.getMultiAz());
    }

}