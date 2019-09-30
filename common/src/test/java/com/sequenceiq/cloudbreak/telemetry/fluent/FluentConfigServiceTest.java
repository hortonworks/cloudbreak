package com.sequenceiq.cloudbreak.telemetry.fluent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3ConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.WasbConfigGenerator;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.FeatureSetting;

public class FluentConfigServiceTest {

    private static final String CLUSTER_TYPE_DEFAULT = "datahub";

    private static final String PLATFORM_DEFAULT = "AWS";

    private FluentConfigService underTest;

    @Before
    public void setUp() {
        underTest = new FluentConfigService(new S3ConfigGenerator(), new WasbConfigGenerator());
    }

    @Test
    public void testCreateFluentConfig() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("mybucket/cluster-logs/datahub/cl1");
        logging.setS3(new S3CloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(CLUSTER_TYPE_DEFAULT, PLATFORM_DEFAULT,
                false, false, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertTrue(result.isCloudStorageLoggingEnabled());
        assertEquals("cluster-logs/datahub/cl1", result.getLogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
    }

    @Test
    public void testCreateFluentConfigWithCustomPath() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("mybucket/custom");
        logging.setS3(new S3CloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(CLUSTER_TYPE_DEFAULT, PLATFORM_DEFAULT,
                false, false, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertTrue(result.isCloudStorageLoggingEnabled());
        assertEquals("custom", result.getLogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
    }

    @Test
    public void testCreateFluentConfigWithS3Path() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("s3://mybucket/cluster-logs/datahub/cl1");
        logging.setS3(new S3CloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(CLUSTER_TYPE_DEFAULT, PLATFORM_DEFAULT,
                false, false, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertTrue(result.isCloudStorageLoggingEnabled());
        assertEquals("cluster-logs/datahub/cl1", result.getLogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
    }

    @Test
    public void testCreateFluentConfigWithS3APath() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("s3a://mybucket/cluster-logs/datahub/cl1");
        logging.setS3(new S3CloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(CLUSTER_TYPE_DEFAULT, PLATFORM_DEFAULT,
                false, false, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertTrue(result.isCloudStorageLoggingEnabled());
        assertEquals("cluster-logs/datahub/cl1", result.getLogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
    }

    @Test
    public void testCreateFluentConfigWithWasbPath() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("wasb://mycontainer@myaccount.blob.core.windows.net");
        WasbCloudStorageV1Parameters wasbParams = new WasbCloudStorageV1Parameters();
        wasbParams.setAccountKey("myAccountKey");
        logging.setWasb(wasbParams);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(CLUSTER_TYPE_DEFAULT, PLATFORM_DEFAULT,
                false, false, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertTrue(result.isCloudStorageLoggingEnabled());
        assertEquals("myAccountKey", result.getAzureStorageAccessKey());
        assertTrue(result.getLogFolderName().isBlank());
        assertEquals("mycontainer", result.getAzureContainer());
    }

    @Test
    public void testCreateFluentConfigWithFullWasbPath() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("wasb://mycontainer@myaccount.blob.core.windows.net/my/custom/path");
        WasbCloudStorageV1Parameters wasbParams = new WasbCloudStorageV1Parameters();
        wasbParams.setAccountKey("myAccountKey");
        wasbParams.setAccountName("myAccount");
        logging.setWasb(wasbParams);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(CLUSTER_TYPE_DEFAULT, PLATFORM_DEFAULT,
                false, false, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("myAccountKey", result.getAzureStorageAccessKey());
        assertEquals("myaccount", result.getAzureStorageAccount());
        assertEquals("/my/custom/path", result.getLogFolderName());
        assertEquals("mycontainer", result.getAzureContainer());
    }

    @Test
    public void testCreateFluentConfigWithFullWasbPathWithContainer() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("wasb://mycontainer/my/custom/path@myaccount.blob.core.windows.net");
        WasbCloudStorageV1Parameters wasbParams = new WasbCloudStorageV1Parameters();
        wasbParams.setAccountKey("myAccountKey");
        logging.setWasb(wasbParams);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(CLUSTER_TYPE_DEFAULT, PLATFORM_DEFAULT,
                false, false, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("myAccountKey", result.getAzureStorageAccessKey());
        assertEquals("/my/custom/path", result.getLogFolderName());
        assertEquals("mycontainer", result.getAzureContainer());
    }

    @Test
    public void testCreateFluentConfigWithoutScheme() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("mycontainer/cluster-logs/datahub/cl1@myaccount.blob.core.windows.net");
        WasbCloudStorageV1Parameters wasbParams = new WasbCloudStorageV1Parameters();
        wasbParams.setAccountKey("myAccountKey");
        logging.setWasb(wasbParams);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(CLUSTER_TYPE_DEFAULT, PLATFORM_DEFAULT,
                false, false, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("myAccountKey", result.getAzureStorageAccessKey());
        assertEquals("/cluster-logs/datahub/cl1", result.getLogFolderName());
        assertEquals("mycontainer", result.getAzureContainer());
    }

    @Test
    public void testCreateFluentConfigMetering() {
        // GIVEN
        Telemetry telemetry = new Telemetry();
        setMetering(telemetry);
        telemetry.setDatabusEndpoint("myEndpoint");
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(CLUSTER_TYPE_DEFAULT, PLATFORM_DEFAULT,
                true, true, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertTrue(result.isMeteringEnabled());
    }

    @Test
    public void testCreateFluentConfigMeteringWithoutDatabusEndpoint() {
        // GIVEN
        Telemetry telemetry = new Telemetry();
        setMetering(telemetry);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(CLUSTER_TYPE_DEFAULT, PLATFORM_DEFAULT,
                false, false, telemetry);
        // THEN
        assertFalse(result.isEnabled());
        assertFalse(result.isMeteringEnabled());
    }

    @Test
    public void testCreateFluentConfigMeteringWithoutDatabusSecret() {
        // GIVEN
        Telemetry telemetry = new Telemetry();
        setMetering(telemetry);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(CLUSTER_TYPE_DEFAULT, PLATFORM_DEFAULT,
                false, false, telemetry);
        // THEN
        assertFalse(result.isEnabled());
        assertFalse(result.isMeteringEnabled());
    }

    @Test
    public void testCreateFluentConfigReportDeploymentLogs() {
        // GIVEN
        Telemetry telemetry = new Telemetry();
        setReportDeploymentLogs(telemetry);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(CLUSTER_TYPE_DEFAULT, PLATFORM_DEFAULT,
                true, false, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertTrue(result.isReportClusterDeploymentLogs());
    }

    @Test
    public void testCreateFluentConfigReportDeploymentLogsWithoutDatabus() {
        // GIVEN
        Telemetry telemetry = new Telemetry();
        setReportDeploymentLogs(telemetry);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(CLUSTER_TYPE_DEFAULT, PLATFORM_DEFAULT,
                false, true, telemetry);
        // THEN
        assertFalse(result.isEnabled());
        assertFalse(result.isReportClusterDeploymentLogs());
    }

    @Test(expected = CloudbreakServiceException.class)
    public void testCreateFluentConfigWithoutLocation() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation(null);
        logging.setWasb(new WasbCloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        underTest.createFluentConfigs(CLUSTER_TYPE_DEFAULT, PLATFORM_DEFAULT,
                false, false, telemetry);
    }

    @Test(expected = CloudbreakServiceException.class)
    public void testCreateFluentConfigWithDoublePath() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("wasb://mycontainer/my/custom/path@myaccount.blob.core.windows.net/my/custom/path");
        WasbCloudStorageV1Parameters wasbParams = new WasbCloudStorageV1Parameters();
        wasbParams.setAccountKey("myAccountKey");
        logging.setWasb(wasbParams);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        underTest.createFluentConfigs(CLUSTER_TYPE_DEFAULT, PLATFORM_DEFAULT,
                false, false, telemetry);
    }

    private void setMetering(Telemetry telemetry) {
        Features features = new Features();
        FeatureSetting metering = new FeatureSetting();
        metering.setEnabled(true);
        features.setMetering(metering);
        telemetry.setFeatures(features);
    }

    private void setReportDeploymentLogs(Telemetry telemetry) {
        Features features = new Features();
        FeatureSetting reportDeploymentLogs = new FeatureSetting();
        reportDeploymentLogs.setEnabled(true);
        features.setReportDeploymentLogs(reportDeploymentLogs);
        telemetry.setFeatures(features);
    }
}
