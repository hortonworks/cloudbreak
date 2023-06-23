package com.sequenceiq.datalake.service.sdx.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

public class DatabaseParameterFallbackUtilTest {

    @Test
    public void testSetupDatabaseInitParams() {
        SdxCluster sdxCluster = new SdxCluster();
        SdxDatabase sdxDatabase = DatabaseParameterFallbackUtil.setupDatabaseInitParams(sdxCluster, SdxDatabaseAvailabilityType.HA, "1.0");
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("1.0", sdxDatabase.getDatabaseEngineVersion());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxCluster.getDatabaseAvailabilityType());
        assertEquals("1.0", sdxCluster.getDatabaseEngineVersion());
    }

    @Test
    public void testSetDatabaseCrn() {
        SdxCluster sdxCluster = new SdxCluster();
        DatabaseParameterFallbackUtil.setDatabaseCrn(sdxCluster, "crn123");
        assertEquals("crn123", sdxCluster.getSdxDatabase().getDatabaseCrn());
        assertEquals("crn123", sdxCluster.getDatabaseCrn());
    }

    @Test
    public void testGetDatabaseCrn() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseCrn("crn456");
        String fallbackDatabaseCrn = "fallback456";
        assertEquals("crn456", DatabaseParameterFallbackUtil.getDatabaseCrn(sdxDatabase, fallbackDatabaseCrn));
        assertEquals(fallbackDatabaseCrn, DatabaseParameterFallbackUtil.getDatabaseCrn(null, fallbackDatabaseCrn));
    }

    @Test
    public void testGetDatabaseEngineVersion() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseEngineVersion("2.0");
        String fallbackDatabaseEngineVersion = "fallback2.0";
        assertEquals("fallback2.0", DatabaseParameterFallbackUtil.getDatabaseEngineVersion(sdxDatabase, fallbackDatabaseEngineVersion));
        assertEquals(fallbackDatabaseEngineVersion, DatabaseParameterFallbackUtil.getDatabaseEngineVersion(null, fallbackDatabaseEngineVersion));
    }

    @Test
    public void testIsCreateDatabase() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setCreateDatabase(true);
        boolean fallbackCreateDatabase = false;
        assertTrue(DatabaseParameterFallbackUtil.isCreateDatabase(sdxDatabase, fallbackCreateDatabase));
        assertFalse(DatabaseParameterFallbackUtil.isCreateDatabase(null, fallbackCreateDatabase));
    }

    @Test
    public void testGetDatabaseAvailabilityType() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        boolean fallbackCreateDatabase = false;
        SdxDatabaseAvailabilityType fallbackDatabaseAvailabilityType = SdxDatabaseAvailabilityType.HA;
        assertEquals(SdxDatabaseAvailabilityType.NONE,
                DatabaseParameterFallbackUtil.getDatabaseAvailabilityType(sdxDatabase, fallbackDatabaseAvailabilityType, fallbackCreateDatabase));
        assertEquals(SdxDatabaseAvailabilityType.HA,
                DatabaseParameterFallbackUtil.getDatabaseAvailabilityType(null, fallbackDatabaseAvailabilityType, true));
        assertEquals(SdxDatabaseAvailabilityType.NONE,
                DatabaseParameterFallbackUtil.getDatabaseAvailabilityType(null, null, false));
        assertEquals(SdxDatabaseAvailabilityType.HA,
                DatabaseParameterFallbackUtil.getDatabaseAvailabilityType(null, null, true));
    }
}
