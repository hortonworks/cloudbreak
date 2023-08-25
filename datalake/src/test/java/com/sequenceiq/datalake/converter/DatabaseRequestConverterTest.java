package com.sequenceiq.datalake.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;

class DatabaseRequestConverterTest {

    private static final String DB_VERSION = "db_version";

    private DatabaseRequestConverter underTest = new DatabaseRequestConverter();

    @Test
    public void testExternalDbMappedVersionNull() {
        SdxCluster sdxCluster = mock(SdxCluster.class);

        DatabaseRequest result = underTest.createExternalDbRequest(sdxCluster);

        assertEquals(DatabaseAvailabilityType.NONE, result.getAvailabilityType());
        assertNull(result.getDatabaseEngineVersion());
    }

    @Test
    public void testNonExternalDbMapping() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getDatabaseEngineVersion()).thenReturn(DB_VERSION);

        DatabaseRequest result = underTest.createExternalDbRequest(sdxCluster);

        assertNotNull(result);
        assertEquals(DB_VERSION, result.getDatabaseEngineVersion());
        assertEquals(DatabaseAvailabilityType.NONE, result.getAvailabilityType());
        verify(sdxCluster).getDatabaseEngineVersion();
    }

    @Test
    public void testAzureDatabaseParams() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getDatabaseEngineVersion()).thenReturn(DB_VERSION);
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setAttributes(new Json("{\"AZURE_DATABASE_TYPE\":\"SINGLE_SERVER\"}"));
        when(sdxCluster.getSdxDatabase()).thenReturn(sdxDatabase);

        DatabaseRequest result = underTest.createExternalDbRequest(sdxCluster);

        assertNotNull(result);
        assertEquals(DB_VERSION, result.getDatabaseEngineVersion());
        assertEquals(DatabaseAvailabilityType.NONE, result.getAvailabilityType());
        assertEquals(AzureDatabaseType.SINGLE_SERVER, result.getDatabaseAzureRequest().getAzureDatabaseType());
        verify(sdxCluster).getDatabaseEngineVersion();
    }

    @Test
    public void testAzureDatabaseParamsForEmptyAttributes() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getDatabaseEngineVersion()).thenReturn(DB_VERSION);

        DatabaseRequest result = underTest.createExternalDbRequest(sdxCluster);

        assertNotNull(result);
        assertNull(result.getDatabaseAzureRequest());
        assertEquals(DB_VERSION, result.getDatabaseEngineVersion());
        assertEquals(DatabaseAvailabilityType.NONE, result.getAvailabilityType());
        verify(sdxCluster).getDatabaseEngineVersion();
    }

    @Test
    public void testAzureDatabaseParamsForNonAzureAttributes() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getDatabaseEngineVersion()).thenReturn(DB_VERSION);
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setAttributes(new Json("{\"CUSTOM_PROPERTY\":\"CUSTOM_VALUE\"}"));
        when(sdxCluster.getSdxDatabase()).thenReturn(sdxDatabase);

        DatabaseRequest result = underTest.createExternalDbRequest(sdxCluster);

        assertNotNull(result);
        assertNull(result.getDatabaseAzureRequest());
        assertEquals(DB_VERSION, result.getDatabaseEngineVersion());
        assertEquals(DatabaseAvailabilityType.NONE, result.getAvailabilityType());
        verify(sdxCluster).getDatabaseEngineVersion();
    }
}