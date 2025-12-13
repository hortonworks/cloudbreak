package com.sequenceiq.datalake.service.sdx.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDatabaseServerV4Parameters;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@ExtendWith(MockitoExtension.class)
public class AwsDatabaseServerParameterSetterTest {

    @Mock
    private DatabaseServerV4StackRequest request;

    @Captor
    private ArgumentCaptor<AwsDatabaseServerV4Parameters> captor;

    private AwsDatabaseServerParameterSetter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new AwsDatabaseServerParameterSetter();
        underTest.backupRetentionPeriodHa = 30;
        underTest.backupRetentionPeriodNonHa = 0;
    }

    @Test
    void testHAServer() {
        underTest.setParameters(request, createSdxDatabase(SdxDatabaseAvailabilityType.HA, null), null, "crn");

        verify(request).setAws(captor.capture());
        AwsDatabaseServerV4Parameters awsDatabaseServerV4Parameters = captor.getValue();
        assertEquals("true", awsDatabaseServerV4Parameters.getMultiAZ());
        assertEquals(30, awsDatabaseServerV4Parameters.getBackupRetentionPeriod());
    }

    @Test
    void testNonHAServer() {
        underTest.setParameters(request, createSdxDatabase(SdxDatabaseAvailabilityType.NON_HA, null), null, "crn");

        verify(request).setAws(captor.capture());
        AwsDatabaseServerV4Parameters awsDatabaseServerV4Parameters = captor.getValue();
        assertEquals("false", awsDatabaseServerV4Parameters.getMultiAZ());
        assertEquals(0, awsDatabaseServerV4Parameters.getBackupRetentionPeriod());
    }

    @Test
    void testEngineVersion() {
        underTest.setParameters(request, createSdxDatabase(SdxDatabaseAvailabilityType.NON_HA, "13"), null, "crn");

        verify(request).setAws(captor.capture());
        AwsDatabaseServerV4Parameters awsDatabaseServerV4Parameters = captor.getValue();
        assertEquals("false", awsDatabaseServerV4Parameters.getMultiAZ());
        assertEquals(0, awsDatabaseServerV4Parameters.getBackupRetentionPeriod());
        assertEquals("13", awsDatabaseServerV4Parameters.getEngineVersion());
    }

    @Test
    void shouldThrowExceptionWhenAvailabilityTypeIsNotSupported() {
        SdxCluster sdxCluster = createSdxDatabase(SdxDatabaseAvailabilityType.NONE, null);
        IllegalArgumentException result =
                assertThrows(IllegalArgumentException.class,
                        () -> underTest.setParameters(request, sdxCluster, null, "crn"));

        assertEquals("NONE database availability type is not supported on AWS.", result.getMessage());
    }

    private SdxCluster createSdxDatabase(SdxDatabaseAvailabilityType sdxDatabaseAvailabilityType, String databaseEngineVersion) {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseAvailabilityType(sdxDatabaseAvailabilityType);
        sdxDatabase.setDatabaseEngineVersion(databaseEngineVersion);

        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setSdxDatabase(sdxDatabase);
        return sdxCluster;
    }
}