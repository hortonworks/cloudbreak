package com.sequenceiq.datalake.service.sdx.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.AzureHighAvailabiltyMode;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@ExtendWith(MockitoExtension.class)
public class AzureDatabaseServerParameterSetterTest {

    @Mock
    private DatabaseServerV4StackRequest request;

    @Mock
    private AzureDatabaseAttributesService azureDatabaseAttributesService;

    @InjectMocks
    private AzureDatabaseServerParameterSetter underTest;

    @Captor
    private ArgumentCaptor<AzureDatabaseServerV4Parameters> captor;

    @BeforeEach
    public void setUp() {
        underTest.geoRedundantBackupHa = true;
        underTest.geoRedundantBackupNonHa = false;
        underTest.backupRetentionPeriodHa = 30;
        underTest.backupRetentionPeriodNonHa = 7;
    }

    @Test
    public void testHAServer() {
        underTest.setParameters(request, createSdxCluster(SdxDatabaseAvailabilityType.HA, null));

        verify(request).setAzure(captor.capture());
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = captor.getValue();
        assertEquals(true, azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals(30, azureDatabaseServerV4Parameters.getBackupRetentionDays());
        assertEquals(AzureHighAvailabiltyMode.SAME_ZONE, azureDatabaseServerV4Parameters.getHighAvailabilityMode());
    }

    @Test
    public void testNonHAServer() {
        underTest.setParameters(request, createSdxCluster(SdxDatabaseAvailabilityType.NON_HA, null));

        verify(request).setAzure(captor.capture());
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = captor.getValue();
        assertEquals(false, azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals(7, azureDatabaseServerV4Parameters.getBackupRetentionDays());
        assertEquals(AzureHighAvailabiltyMode.DISABLED, azureDatabaseServerV4Parameters.getHighAvailabilityMode());
    }

    @Test
    public void testEngineVersion() {
        underTest.setParameters(request, createSdxCluster(SdxDatabaseAvailabilityType.NON_HA, "13"));

        verify(request).setAzure(captor.capture());
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = captor.getValue();
        assertEquals(false, azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals(7, azureDatabaseServerV4Parameters.getBackupRetentionDays());
        assertEquals("13", azureDatabaseServerV4Parameters.getDbVersion());
        assertEquals(AzureHighAvailabiltyMode.DISABLED, azureDatabaseServerV4Parameters.getHighAvailabilityMode());
    }

    @ParameterizedTest
    @EnumSource(AzureDatabaseType.class)
    public void testAzureDatabaseType(AzureDatabaseType azureDatabaseType) {
        when(azureDatabaseAttributesService.getAzureDatabaseType(any(SdxDatabase.class))).thenReturn(azureDatabaseType);
        underTest.setParameters(request, createSdxCluster(SdxDatabaseAvailabilityType.NON_HA, "13"));
        verify(request).setAzure(captor.capture());
        assertEquals(azureDatabaseType, captor.getValue().getAzureDatabaseType());
    }

    @Test
    public void shouldThrowExceptionWhenAvailabilityTypeIsNotSupported() {
        IllegalArgumentException result =
                Assertions.assertThrows(IllegalArgumentException.class,
                        () -> underTest.setParameters(request, createSdxCluster(SdxDatabaseAvailabilityType.NONE, null)));

        assertEquals("NONE database availability type is not supported on Azure.", result.getMessage());
    }

    private SdxCluster createSdxCluster(SdxDatabaseAvailabilityType sdxDatabaseAvailabilityType, String databaseEngineVeersion) {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseAvailabilityType(sdxDatabaseAvailabilityType);
        sdxDatabase.setDatabaseEngineVersion(databaseEngineVeersion);
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setSdxDatabase(sdxDatabase);
        return sdxCluster;
    }
}
