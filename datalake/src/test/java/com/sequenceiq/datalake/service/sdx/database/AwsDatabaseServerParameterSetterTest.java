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
    public void testHAServer() {
        underTest.setParameters(request, SdxDatabaseAvailabilityType.HA, null);

        verify(request).setAws(captor.capture());
        AwsDatabaseServerV4Parameters awsDatabaseServerV4Parameters = captor.getValue();
        assertEquals("true", awsDatabaseServerV4Parameters.getMultiAZ());
        assertEquals(30, awsDatabaseServerV4Parameters.getBackupRetentionPeriod());
    }

    @Test
    public void testNonHAServer() {
        underTest.setParameters(request, SdxDatabaseAvailabilityType.NON_HA, null);

        verify(request).setAws(captor.capture());
        AwsDatabaseServerV4Parameters awsDatabaseServerV4Parameters = captor.getValue();
        assertEquals("false", awsDatabaseServerV4Parameters.getMultiAZ());
        assertEquals(0, awsDatabaseServerV4Parameters.getBackupRetentionPeriod());
    }

    @Test
    public void testEngineVersion() {
        underTest.setParameters(request, SdxDatabaseAvailabilityType.NON_HA, "13");

        verify(request).setAws(captor.capture());
        AwsDatabaseServerV4Parameters awsDatabaseServerV4Parameters = captor.getValue();
        assertEquals("false", awsDatabaseServerV4Parameters.getMultiAZ());
        assertEquals(0, awsDatabaseServerV4Parameters.getBackupRetentionPeriod());
        assertEquals("13", awsDatabaseServerV4Parameters.getEngineVersion());
    }

    @Test
    public void shouldThrowExceptionWhenAvailabilityTypeIsNotSupported() {
        IllegalArgumentException result =
                Assertions.assertThrows(IllegalArgumentException.class, () -> underTest.setParameters(request, SdxDatabaseAvailabilityType.NONE, null));

        assertEquals("NONE database availability type is not supported on AWS.", result.getMessage());
    }
}