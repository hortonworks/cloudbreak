package com.sequenceiq.datalake.service.sdx.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@ExtendWith(MockitoExtension.class)
public class AzureDatabaseServerParameterSetterTest {

    @Mock
    private DatabaseServerV4StackRequest request;

    @Captor
    private ArgumentCaptor<AzureDatabaseServerV4Parameters> captor;

    private AzureDatabaseServerParameterSetter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new AzureDatabaseServerParameterSetter();
        underTest.geoRedundantBackupHa = true;
        underTest.geoRedundantBackupNonHa = false;
        underTest.backupRetentionPeriodHa = 30;
        underTest.backupRetentionPeriodNonHa = 7;
    }

    @Test
    public void testHAServer() {
        underTest.setParameters(request, SdxDatabaseAvailabilityType.HA);

        verify(request).setAzure(captor.capture());
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = captor.getValue();
        assertEquals(true, azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals(30, azureDatabaseServerV4Parameters.getBackupRetentionDays());
    }

    @Test
    public void testNonHAServer() {
        underTest.setParameters(request, SdxDatabaseAvailabilityType.NON_HA);

        verify(request).setAzure(captor.capture());
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = captor.getValue();
        assertEquals(false, azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals(7, azureDatabaseServerV4Parameters.getBackupRetentionDays());
    }

    @Test
    public void shouldThrowExceptionWhenAvailabilityTypeIsNotSupported() {
        IllegalArgumentException result =
                Assertions.assertThrows(IllegalArgumentException.class, () -> underTest.setParameters(request, SdxDatabaseAvailabilityType.NONE));

        assertEquals("NONE database availability type is not supported on Azure.", result.getMessage());
    }
}