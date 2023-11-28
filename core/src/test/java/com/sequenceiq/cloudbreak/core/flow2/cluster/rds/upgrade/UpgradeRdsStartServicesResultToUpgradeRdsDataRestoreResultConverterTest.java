package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataRestoreResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartServicesResult;

class UpgradeRdsStartServicesResultToUpgradeRdsDataRestoreResultConverterTest {
    private final UpgradeRdsStartServicesResultToUpgradeRdsDataRestoreResultConverter converter =
            new UpgradeRdsStartServicesResultToUpgradeRdsDataRestoreResultConverter();

    @Test
    public void testCanConvert() {
        assertTrue(converter.canConvert(UpgradeRdsStartServicesResult.class));
        assertFalse(converter.canConvert(String.class));
    }

    @Test
    public void testConvert() {
        UpgradeRdsStartServicesResult source = new UpgradeRdsStartServicesResult(1L, TargetMajorVersion.VERSION14);
        UpgradeRdsDataRestoreResult result = converter.convert(source);
        assertNotNull(result);
        assertEquals(1L, result.getResourceId());
        assertEquals(TargetMajorVersion.VERSION14, result.getVersion());
    }
}
