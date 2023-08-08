package com.sequenceiq.datalake.service.sdx.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

public class DatabaseParameterFallbackUtilTest {

    @Test
    public void testSetupDatabaseInitParams() {
        SdxCluster sdxCluster = new SdxCluster();
        SdxDatabase sdxDatabase = DatabaseParameterFallbackUtil.setupDatabaseInitParams(SdxDatabaseAvailabilityType.HA, "1.0");
        sdxCluster.setSdxDatabase(sdxDatabase);
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("1.0", sdxDatabase.getDatabaseEngineVersion());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxCluster.getDatabaseAvailabilityType());
        assertEquals("1.0", sdxCluster.getDatabaseEngineVersion());
    }

    @Test
    public void testSetDatabaseCrn() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setSdxDatabase(new SdxDatabase());
        DatabaseParameterFallbackUtil.setDatabaseCrn(sdxCluster, "crn123");
        assertEquals("crn123", sdxCluster.getSdxDatabase().getDatabaseCrn());
        assertEquals("crn123", sdxCluster.getDatabaseCrn());
    }

    @Test
    public void testGetDatabaseAvailabilityType() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        assertEquals(SdxDatabaseAvailabilityType.NONE,
                DatabaseParameterFallbackUtil.getDatabaseAvailabilityType(SdxDatabaseAvailabilityType.NONE, false));
        assertEquals(SdxDatabaseAvailabilityType.HA,
                DatabaseParameterFallbackUtil.getDatabaseAvailabilityType(SdxDatabaseAvailabilityType.HA, true));
        assertEquals(SdxDatabaseAvailabilityType.NONE,
                DatabaseParameterFallbackUtil.getDatabaseAvailabilityType(null, false));
        assertEquals(SdxDatabaseAvailabilityType.HA,
                DatabaseParameterFallbackUtil.getDatabaseAvailabilityType(null, true));
    }
}
