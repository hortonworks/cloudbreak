package com.sequenceiq.cloudbreak.telemetry.fluent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfiguration;
import com.sequenceiq.cloudbreak.telemetry.common.AnonymizationRuleResolver;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.GcsConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3ConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.logcollection.ClusterLogsCollectionConfiguration;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConfiguration;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public class FluentConfigServiceTest {

    private static final String CLUSTER_TYPE_DEFAULT = "datahub";

    private static final String PLATFORM_DEFAULT = "AWS";

    private static final String REGION_SAMPLE = "eu-central-1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:eu-1:1234:user:91011";

    private static final TelemetryClusterDetails DEFAULT_FLUENT_CLUSTER_DETAILS =
            TelemetryClusterDetails.Builder.builder().withType(CLUSTER_TYPE_DEFAULT).withPlatform(PLATFORM_DEFAULT).withCrn(DATAHUB_CRN).build();

    private FluentConfigService underTest;

    @Before
    public void setUp() {
        MeteringConfiguration meteringConfiguration =
                new MeteringConfiguration(false, null, null);
        ClusterLogsCollectionConfiguration logCollectionConfig =
                new ClusterLogsCollectionConfiguration(false, null, null);
        TelemetryConfiguration telemetryConfiguration =
                new TelemetryConfiguration(null, meteringConfiguration, logCollectionConfig, null, null);
        underTest = new FluentConfigService(new S3ConfigGenerator(), new AdlsGen2ConfigGenerator(), new GcsConfigGenerator(),
                new AnonymizationRuleResolver(), telemetryConfiguration);
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
        FluentConfigView result = underTest.createFluentConfigs(
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, REGION_SAMPLE, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertTrue(result.isCloudStorageLoggingEnabled());
        assertEquals("cluster-logs/datahub/cl1", result.getLogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
        assertEquals(Crn.Region.EU_1.getName(), result.toMap().get("environmentRegion"));
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
        FluentConfigView result = underTest.createFluentConfigs(
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, REGION_SAMPLE, telemetry);
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
        FluentConfigView result = underTest.createFluentConfigs(
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, REGION_SAMPLE, telemetry);
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
        FluentConfigView result = underTest.createFluentConfigs(
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, REGION_SAMPLE, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertTrue(result.isCloudStorageLoggingEnabled());
        assertEquals("cluster-logs/datahub/cl1", result.getLogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
    }

    @Test
    public void testCreateFluentConfigWitGcsPath() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("gs://mybucket/cluster-logs/datahub/cl1");
        GcsCloudStorageV1Parameters gcsParams = new GcsCloudStorageV1Parameters();
        gcsParams.setServiceAccountEmail("myaccount@myprojectid.iam.gserviceaccount.com");
        logging.setGcs(gcsParams);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, REGION_SAMPLE, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertTrue(result.isCloudStorageLoggingEnabled());
        assertEquals("cluster-logs/datahub/cl1", result.getLogFolderName());
        assertEquals("mybucket", result.getGcsBucket());
        assertEquals("myprojectid", result.getGcsProjectId());
    }

    @Test
    public void testCreateFluentConfigWithAdlsGen2Path() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("abfs://mycontainer@myaccount.dfs.core.windows.net");
        AdlsGen2CloudStorageV1Parameters parameters = new AdlsGen2CloudStorageV1Parameters();
        parameters.setAccountKey("myAccountKey");
        logging.setAdlsGen2(parameters);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, REGION_SAMPLE, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertTrue(result.isCloudStorageLoggingEnabled());
        assertEquals("myAccountKey", result.getAzureStorageAccessKey());
        assertTrue(result.getLogFolderName().isBlank());
        assertEquals("mycontainer", result.getAzureContainer());
    }

    @Test
    public void testCreateFluentConfigWithFullAdlsGen2Path() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("abfs://mycontainer@myaccount.dfs.core.windows.net/my/custom/path");
        AdlsGen2CloudStorageV1Parameters parameters = new AdlsGen2CloudStorageV1Parameters();
        parameters.setAccountKey("myAccountKey");
        parameters.setAccountName("myAccount");
        logging.setAdlsGen2(parameters);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, REGION_SAMPLE, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("myAccountKey", result.getAzureStorageAccessKey());
        assertEquals("myaccount", result.getAzureStorageAccount());
        assertEquals("/my/custom/path", result.getLogFolderName());
        assertEquals("mycontainer", result.getAzureContainer());
    }

    @Test
    public void testCreateFluentConfigWithFullAdlsGen2PathWithContainer() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("abfs://mycontainer/my/custom/path@myaccount.dfs.core.windows.net");
        AdlsGen2CloudStorageV1Parameters parameters = new AdlsGen2CloudStorageV1Parameters();
        parameters.setAccountKey("myAccountKey");
        logging.setAdlsGen2(parameters);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, REGION_SAMPLE, telemetry);
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
        logging.setStorageLocation("mycontainer/cluster-logs/datahub/cl1@myaccount.dfs.core.windows.net");
        AdlsGen2CloudStorageV1Parameters parameters = new AdlsGen2CloudStorageV1Parameters();
        parameters.setAccountKey("myAccountKey");
        logging.setAdlsGen2(parameters);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, REGION_SAMPLE, telemetry);
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
        FluentConfigView result = underTest.createFluentConfigs(
                DEFAULT_FLUENT_CLUSTER_DETAILS, true, true, REGION_SAMPLE, telemetry);
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
        FluentConfigView result = underTest.createFluentConfigs(
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, REGION_SAMPLE, telemetry);
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
        FluentConfigView result = underTest.createFluentConfigs(DEFAULT_FLUENT_CLUSTER_DETAILS, false, false,
                REGION_SAMPLE, telemetry);
        // THEN
        assertFalse(result.isEnabled());
        assertFalse(result.isMeteringEnabled());
    }

    @Test
    public void testCreateFluentConfigClusterLogsCollection() {
        // GIVEN
        Telemetry telemetry = new Telemetry();
        setClusterLogsCollection(telemetry);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(DEFAULT_FLUENT_CLUSTER_DETAILS, true, false,
                REGION_SAMPLE, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertTrue(result.isClusterLogsCollection());
    }

    @Test
    public void testCreateFluentConfigClusterLogsCollectionWithoutDatabus() {
        // GIVEN
        Telemetry telemetry = new Telemetry();
        setClusterLogsCollection(telemetry);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, true,
                REGION_SAMPLE, telemetry);
        // THEN
        assertFalse(result.isEnabled());
        assertFalse(result.isClusterLogsCollection());
    }

    @Test(expected = CloudbreakServiceException.class)
    public void testCreateFluentConfigWithoutLocation() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation(null);
        logging.setAdlsGen2(new AdlsGen2CloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        underTest.createFluentConfigs(DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, REGION_SAMPLE, telemetry);
    }

    @Test(expected = CloudbreakServiceException.class)
    public void testCreateFluentConfigWithoutGcsProjectId() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("gs://mybucket/mypath");
        logging.setGcs(new GcsCloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        underTest.createFluentConfigs(DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, REGION_SAMPLE, telemetry);
    }

    @Test(expected = CloudbreakServiceException.class)
    public void testCreateFluentConfigWithDoublePath() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("abfs://mycontainer/my/custom/path@myaccount.dfs.core.windows.net/my/custom/path");
        AdlsGen2CloudStorageV1Parameters parameters = new AdlsGen2CloudStorageV1Parameters();
        parameters.setAccountKey("myAccountKey");
        logging.setAdlsGen2(parameters);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        underTest.createFluentConfigs(DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, REGION_SAMPLE, telemetry);
    }

    private void setMetering(Telemetry telemetry) {
        Features features = new Features();
        features.addMetering(true);
        telemetry.setFeatures(features);
    }

    private void setClusterLogsCollection(Telemetry telemetry) {
        Features features = new Features();
        features.addClusterLogsCollection(true);
        telemetry.setFeatures(features);
    }
}