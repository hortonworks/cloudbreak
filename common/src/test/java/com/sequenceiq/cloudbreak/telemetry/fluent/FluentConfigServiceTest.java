package com.sequenceiq.cloudbreak.telemetry.fluent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3ConfigGenerator;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public class FluentConfigServiceTest {

    private static final String CLUSTER_TYPE_DEFAULT = "datahub";

    private static final String PLATFORM_DEFAULT = "AWS";

    private static final TelemetryClusterDetails DEFAULT_FLUENT_CLUSTER_DETAILS =
            TelemetryClusterDetails.Builder.builder().withType(CLUSTER_TYPE_DEFAULT).withPlatform(PLATFORM_DEFAULT).build();

    private FluentConfigService underTest;

    @Before
    public void setUp() {
        underTest = new FluentConfigService(new S3ConfigGenerator(), new AdlsGen2ConfigGenerator());
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
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, telemetry);
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
        FluentConfigView result = underTest.createFluentConfigs(
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, telemetry);
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
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, telemetry);
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
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertTrue(result.isCloudStorageLoggingEnabled());
        assertEquals("cluster-logs/datahub/cl1", result.getLogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
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
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, telemetry);
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
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, telemetry);
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
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, telemetry);
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
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, telemetry);
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
                DEFAULT_FLUENT_CLUSTER_DETAILS, true, true, telemetry);
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
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, telemetry);
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
        FluentConfigView result = underTest.createFluentConfigs(DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, telemetry);
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
        FluentConfigView result = underTest.createFluentConfigs(DEFAULT_FLUENT_CLUSTER_DETAILS, true, false, telemetry);
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
                DEFAULT_FLUENT_CLUSTER_DETAILS, false, true, telemetry);
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
        underTest.createFluentConfigs(DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, telemetry);
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
        underTest.createFluentConfigs(DEFAULT_FLUENT_CLUSTER_DETAILS, false, false, telemetry);
    }

    @Test
    public void testDecodeAnonymizationRule() {
        // GIVEN
        List<AnonymizationRule> rules = new ArrayList<>();
        AnonymizationRule rule1 = new AnonymizationRule();
        rule1.setReplacement("replace1");
        AnonymizationRule rule2 = new AnonymizationRule();
        rule2.setReplacement("replace2");
        rule2.setValue(Base64.getEncoder().encodeToString("value2".getBytes()));
        rules.add(rule1);
        rules.add(rule2);
        // WHEN
        List<AnonymizationRule> finalRules = underTest.decodeRules(rules);
        // THEN
        assertEquals(finalRules.size(), 1);
        assertEquals(finalRules.get(0).getValue(), "value2");
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