package com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.AzureHighAvailabiltyMode;

class AzureDatabaseServerV4ParametersTest {

    private AzureDatabaseServerV4Parameters underTest;

    @BeforeEach
    public void setUp() {
        underTest = new AzureDatabaseServerV4Parameters();
    }

    @Test
    void testGettersAndSetters() {
        underTest.setBackupRetentionDays(3);
        assertThat(underTest.getBackupRetentionDays().intValue()).isEqualTo(3);

        underTest.setGeoRedundantBackup(true);
        assertThat(underTest.getGeoRedundantBackup()).isTrue();

        underTest.setSkuCapacity(5);
        assertThat(underTest.getSkuCapacity().intValue()).isEqualTo(5);

        underTest.setSkuFamily("some-family");
        assertThat(underTest.getSkuFamily()).isEqualTo("some-family");

        underTest.setSkuTier("some-tier");
        assertThat(underTest.getSkuTier()).isEqualTo("some-tier");

        underTest.setStorageAutoGrow(true);
        assertThat(underTest.getStorageAutoGrow()).isTrue();

        underTest.setDbVersion("1.2.3");
        assertThat(underTest.getDbVersion()).isEqualTo("1.2.3");
    }

    @Test
    void testAsMap() {
        underTest.setBackupRetentionDays(3);
        underTest.setDbVersion("1.2.3");

        assertThat(underTest.asMap()).containsOnly(
                Map.entry("backupRetentionDays", 3),
                Map.entry("dbVersion", "1.2.3"),
                Map.entry("geoRedundantBackup", false),
                Map.entry("cloudPlatform", "AZURE"),
                Map.entry(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.SINGLE_SERVER.name()),
                Map.entry(AzureHighAvailabiltyMode.AZURE_HA_MODE_KEY, AzureHighAvailabiltyMode.DISABLED.name()));

        underTest.setSkuCapacity(5);
        underTest.setSkuFamily("some-family");
        underTest.setSkuTier("some-tier");
        underTest.setGeoRedundantBackup(true);
        underTest.setStorageAutoGrow(true);

        assertThat(underTest.asMap()).containsOnly(Map.entry("backupRetentionDays", 3),
                Map.entry("dbVersion", "1.2.3"),
                Map.entry("cloudPlatform", "AZURE"),
                Map.entry("skuCapacity", 5),
                Map.entry("skuFamily", "some-family"),
                Map.entry("skuTier", "some-tier"),
                Map.entry("geoRedundantBackup", true),
                Map.entry("storageAutoGrow", true),
                Map.entry(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.SINGLE_SERVER.name()),
                Map.entry(AzureHighAvailabiltyMode.AZURE_HA_MODE_KEY, AzureHighAvailabiltyMode.DISABLED.name()));

        underTest.setAzureDatabaseType(AzureDatabaseType.FLEXIBLE_SERVER);
        underTest.setHighAvailabilityMode(AzureHighAvailabiltyMode.SAME_ZONE);

        assertThat(underTest.asMap()).containsOnly(Map.entry("backupRetentionDays", 3),
                Map.entry("dbVersion", "1.2.3"),
                Map.entry("cloudPlatform", "AZURE"),
                Map.entry("skuCapacity", 5),
                Map.entry("skuFamily", "some-family"),
                Map.entry("skuTier", "some-tier"),
                Map.entry("geoRedundantBackup", true),
                Map.entry("storageAutoGrow", true),
                Map.entry(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER.name()),
                Map.entry(AzureHighAvailabiltyMode.AZURE_HA_MODE_KEY, AzureHighAvailabiltyMode.SAME_ZONE.name()));
    }

    @Test
    void testGetCloudPlatform() {
        assertThat(underTest.getCloudPlatform()).isEqualTo(CloudPlatform.AZURE);
    }

    @Test
    void testParse() {
        Map<String, Object> parameters = ImmutableMap.<String, Object>builder()
                .put("backupRetentionDays", 3)
                .put("dbVersion", "1.2.3")
                .put("skuCapacity", 5)
                .put("skuFamily", "some-family")
                .put("skuTier", "some-tier")
                .put("geoRedundantBackup", true)
                .put("storageAutoGrow", true)
                .put("highAvailability", AzureHighAvailabiltyMode.DISABLED.name())
                .build();

        underTest.parse(parameters);

        assertThat(underTest.getBackupRetentionDays().intValue()).isEqualTo(3);
        assertThat(underTest.getDbVersion()).isEqualTo("1.2.3");
        assertThat(underTest.getSkuCapacity().intValue()).isEqualTo(5);
        assertThat(underTest.getSkuFamily()).isEqualTo("some-family");
        assertThat(underTest.getSkuTier()).isEqualTo("some-tier");
        assertThat(underTest.getGeoRedundantBackup()).isTrue();
        assertThat(underTest.getStorageAutoGrow()).isTrue();
        assertThat(underTest.getAzureDatabaseType()).isEqualTo(AzureDatabaseType.SINGLE_SERVER);
        assertThat(underTest.getHighAvailabilityMode()).isEqualTo(AzureHighAvailabiltyMode.DISABLED);
    }

}
