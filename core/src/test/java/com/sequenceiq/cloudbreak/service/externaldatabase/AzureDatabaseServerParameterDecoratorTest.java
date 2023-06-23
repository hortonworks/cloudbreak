package com.sequenceiq.cloudbreak.service.externaldatabase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.AzureHighAvailabiltyMode;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;

class AzureDatabaseServerParameterDecoratorTest {
    private final AzureDatabaseServerParameterDecorator underTest = new AzureDatabaseServerParameterDecorator();

    @Test
    void testSetParametersHa() {
        ReflectionTestUtils.setField(underTest, "retentionPeriodHa", 2);
        ReflectionTestUtils.setField(underTest, "geoRedundantBackupHa", Boolean.TRUE);
        ReflectionTestUtils.setField(underTest, "retentionPeriodNonHa", 1);
        ReflectionTestUtils.setField(underTest, "geoRedundantBackupNonHa", Boolean.FALSE);
        DatabaseServerV4StackRequest databaseServerV4StackRequest = new DatabaseServerV4StackRequest();
        DatabaseServerParameter databaseServerParameter = DatabaseServerParameter.builder()
                .withAvailabilityType(DatabaseAvailabilityType.HA)
                .withEngineVersion("11")
                .withAttributes(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.SINGLE_SERVER.name()))
                .build();
        underTest.setParameters(databaseServerV4StackRequest, databaseServerParameter);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = databaseServerV4StackRequest.getAzure();
        assertEquals(2, azureDatabaseServerV4Parameters.getBackupRetentionDays());
        assertTrue(azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals("11", azureDatabaseServerV4Parameters.getDbVersion());
        assertEquals(AzureDatabaseType.SINGLE_SERVER, azureDatabaseServerV4Parameters.getAzureDatabaseType());
        assertEquals(AzureHighAvailabiltyMode.SAME_ZONE, azureDatabaseServerV4Parameters.getHighAvailabilityMode());
    }

    @Test
    void testSetParametersNonHa() {
        ReflectionTestUtils.setField(underTest, "retentionPeriodHa", 2);
        ReflectionTestUtils.setField(underTest, "geoRedundantBackupHa", Boolean.TRUE);
        ReflectionTestUtils.setField(underTest, "retentionPeriodNonHa", 1);
        ReflectionTestUtils.setField(underTest, "geoRedundantBackupNonHa", Boolean.FALSE);
        DatabaseServerV4StackRequest databaseServerV4StackRequest = new DatabaseServerV4StackRequest();
        DatabaseServerParameter databaseServerParameter = DatabaseServerParameter.builder()
                .withAvailabilityType(DatabaseAvailabilityType.NONE)
                .withEngineVersion("11")
                .withAttributes(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER.name()))
                .build();
        underTest.setParameters(databaseServerV4StackRequest, databaseServerParameter);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = databaseServerV4StackRequest.getAzure();
        assertEquals(1, azureDatabaseServerV4Parameters.getBackupRetentionDays());
        assertFalse(azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals("11", azureDatabaseServerV4Parameters.getDbVersion());
        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, azureDatabaseServerV4Parameters.getAzureDatabaseType());
        assertEquals(AzureHighAvailabiltyMode.DISABLED, azureDatabaseServerV4Parameters.getHighAvailabilityMode());
    }

    @Test
    void testSetParametersNonHaNoAttributes() {
        ReflectionTestUtils.setField(underTest, "retentionPeriodHa", 2);
        ReflectionTestUtils.setField(underTest, "geoRedundantBackupHa", Boolean.TRUE);
        ReflectionTestUtils.setField(underTest, "retentionPeriodNonHa", 1);
        ReflectionTestUtils.setField(underTest, "geoRedundantBackupNonHa", Boolean.FALSE);
        DatabaseServerV4StackRequest databaseServerV4StackRequest = new DatabaseServerV4StackRequest();
        DatabaseServerParameter databaseServerParameter = DatabaseServerParameter.builder()
                .withAvailabilityType(DatabaseAvailabilityType.NON_HA)
                .withEngineVersion("11")
                .build();
        underTest.setParameters(databaseServerV4StackRequest, databaseServerParameter);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = databaseServerV4StackRequest.getAzure();
        assertEquals(1, azureDatabaseServerV4Parameters.getBackupRetentionDays());
        assertFalse(azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals("11", azureDatabaseServerV4Parameters.getDbVersion());
        assertEquals(AzureDatabaseType.SINGLE_SERVER, azureDatabaseServerV4Parameters.getAzureDatabaseType());
        assertEquals(AzureHighAvailabiltyMode.DISABLED, azureDatabaseServerV4Parameters.getHighAvailabilityMode());
    }
}
