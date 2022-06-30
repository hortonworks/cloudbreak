package com.sequenceiq.datalake.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.datalake.entity.SdxCluster;

class DatabaseRequestConverterTest {

    private static final String DB_VERSION = "db_version";

    private DatabaseRequestConverter underTest = new DatabaseRequestConverter();

    @Test
    public void testExternalDbMappedToNull() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.hasExternalDatabase()).thenReturn(Boolean.TRUE);

        DatabaseRequest result = underTest.createExternalDbRequest(sdxCluster);

        assertNull(result);
        verify(sdxCluster).hasExternalDatabase();
        verifyNoMoreInteractions(sdxCluster);
    }

    @Test
    public void testNonExternalDbMapping() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.hasExternalDatabase()).thenReturn(Boolean.FALSE);
        when(sdxCluster.getDatabaseEngineVersion()).thenReturn(DB_VERSION);

        DatabaseRequest result = underTest.createExternalDbRequest(sdxCluster);

        assertNotNull(result);
        assertEquals(DB_VERSION, result.getDatabaseEngineVersion());
        assertEquals(DatabaseAvailabilityType.NONE, result.getAvailabilityType());
        verify(sdxCluster).hasExternalDatabase();
        verify(sdxCluster).getDatabaseEngineVersion();
        verifyNoMoreInteractions(sdxCluster);
    }
}