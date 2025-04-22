package com.sequenceiq.cloudbreak.telemetry.fluent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfiguration;
import com.sequenceiq.cloudbreak.telemetry.common.AnonymizationRuleResolver;
import com.sequenceiq.cloudbreak.telemetry.context.LogShipperContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.GcsConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3ConfigGenerator;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public class FluentConfigServiceTest {

    private static final String CLUSTER_TYPE_DEFAULT = "datahub";

    private static final String PLATFORM_DEFAULT = "AWS";

    private static final String REGION_SAMPLE = "eu-central-1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:eu-1:1234:user:91011";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:default:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final TelemetryClusterDetails DEFAULT_FLUENT_CLUSTER_DETAILS =
            TelemetryClusterDetails.Builder.builder().withType(CLUSTER_TYPE_DEFAULT).withPlatform(PLATFORM_DEFAULT)
                    .withCrn(DATAHUB_CRN).withEnvironmentCrn(ENVIRONMENT_CRN).build();

    private FluentConfigService underTest;

    @BeforeEach
    public void setUp() {
        TelemetryConfiguration telemetryConfiguration =
                new TelemetryConfiguration(null, null, null);
        underTest = new FluentConfigService(new S3ConfigGenerator(), new AdlsGen2ConfigGenerator(), new GcsConfigGenerator(),
                new AnonymizationRuleResolver(), telemetryConfiguration);
    }

    @Test
    public void testIsEnabled() {
        // GIVEN
        // WHEN
        boolean result = underTest.isEnabled(telemetryContext());
        // THEN
        assertTrue(result);
    }

    @Test
    public void testIsEnabledWithoutLogAndMeteringContext() {
        // GIVEN
        TelemetryContext context = telemetryContext();
        context.setLogShipperContext(null);
        // WHEN
        boolean result = underTest.isEnabled(context);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testIsEnabledWithoutContext() {
        // GIVEN
        // WHEN
        boolean result = underTest.isEnabled(null);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testCreateConfigs() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("mybucket/cluster-logs/datahub/cl1");
        logging.setS3(new S3CloudStorageV1Parameters());
        // WHEN
        Map<String, Object> result = underTest.createConfigs(telemetryContext(logging)).toMap();
        // THEN
        assertEquals(true, result.get("enabled"));
        assertEquals(true, result.get("cloudStorageLoggingEnabled"));
        assertEquals("mybucket", result.get("s3LogArchiveBucketName"));
        assertEquals("cluster-logs/datahub/cl1", result.get("logFolderName"));
        assertEquals("s3", result.get("providerPrefix"));
        assertEquals("eu-1", result.get("environmentRegion"));
    }

    @Test
    public void testCreateConfigsWithS3Scheme() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("s3://mybucket/cluster-logs/datahub/cl1");
        logging.setS3(new S3CloudStorageV1Parameters());
        // WHEN
        Map<String, Object> result = underTest.createConfigs(telemetryContext(logging)).toMap();
        // THEN
        assertEquals(true, result.get("enabled"));
        assertEquals(true, result.get("cloudStorageLoggingEnabled"));
        assertEquals("mybucket", result.get("s3LogArchiveBucketName"));
        assertEquals("cluster-logs/datahub/cl1", result.get("logFolderName"));
        assertEquals("s3", result.get("providerPrefix"));
    }

    @Test
    public void testCreateConfigsWithGcs() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("gs://mybucket/cluster-logs/datahub/cl1");
        GcsCloudStorageV1Parameters gcsParams = new GcsCloudStorageV1Parameters();
        gcsParams.setServiceAccountEmail("myaccount@myprojectid.iam.gserviceaccount.com");
        logging.setGcs(gcsParams);
        // WHEN
        Map<String, Object> result = underTest.createConfigs(telemetryContext(logging)).toMap();
        // THEN
        assertEquals(true, result.get("enabled"));
        assertEquals(true, result.get("cloudStorageLoggingEnabled"));
        assertEquals("cluster-logs/datahub/cl1", result.get("logFolderName"));
        assertEquals("mybucket", result.get("gcsBucket"));
        assertEquals("myprojectid", result.get("gcsProjectId"));
        assertEquals("gcs", result.get("providerPrefix"));
    }

    @Test
    public void testCreateConfigsWithAbfs() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("abfs://mycontainer@myaccount.dfs.core.windows.net");
        AdlsGen2CloudStorageV1Parameters parameters = new AdlsGen2CloudStorageV1Parameters();
        parameters.setAccountKey("myAccountKey");
        parameters.setAccountName("myAccount");
        logging.setAdlsGen2(parameters);
        // WHEN
        Map<String, Object> result = underTest.createConfigs(telemetryContext(logging)).toMap();
        // THEN
        assertEquals(true, result.get("enabled"));
        assertEquals(true, result.get("cloudStorageLoggingEnabled"));
        assertEquals("", result.get("logFolderName"));
        assertEquals("myAccountKey", result.get("azureStorageAccessKey"));
        assertEquals("myaccount", result.get("azureStorageAccount"));
        assertEquals("mycontainer", result.get("azureContainer"));
    }

    @Test
    public void testCreateConfigsWithAbfsFullPath() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("abfs://mycontainer@myaccount.dfs.core.windows.net/my/custom/path");
        AdlsGen2CloudStorageV1Parameters parameters = new AdlsGen2CloudStorageV1Parameters();
        parameters.setAccountKey("myAccountKey");
        parameters.setAccountName("myAccount");
        logging.setAdlsGen2(parameters);
        // WHEN
        Map<String, Object> result = underTest.createConfigs(telemetryContext(logging)).toMap();
        // THEN
        assertEquals(true, result.get("enabled"));
        assertEquals(true, result.get("cloudStorageLoggingEnabled"));
        assertEquals("/my/custom/path", result.get("logFolderName"));
        assertEquals("myAccountKey", result.get("azureStorageAccessKey"));
        assertEquals("myaccount", result.get("azureStorageAccount"));
        assertEquals("mycontainer", result.get("azureContainer"));
    }

    @Test
    public void testCreateConfigsWithAbfsContainerPath() {
        // GIVEN
        Logging logging = new Logging();
        logging.setStorageLocation("abfs://mycontainer/my/custom/path@myaccount.dfs.core.windows.net");
        AdlsGen2CloudStorageV1Parameters parameters = new AdlsGen2CloudStorageV1Parameters();
        parameters.setAccountKey("myAccountKey");
        parameters.setAccountName("myAccount");
        logging.setAdlsGen2(parameters);
        // WHEN
        Map<String, Object> result = underTest.createConfigs(telemetryContext(logging)).toMap();
        // THEN
        assertEquals(true, result.get("enabled"));
        assertEquals(true, result.get("cloudStorageLoggingEnabled"));
        assertEquals("/my/custom/path", result.get("logFolderName"));
        assertEquals("myAccountKey", result.get("azureStorageAccessKey"));
        assertEquals("myaccount", result.get("azureStorageAccount"));
        assertEquals("mycontainer", result.get("azureContainer"));
    }

    private TelemetryContext telemetryContext() {
        return telemetryContext(null);
    }

    private TelemetryContext telemetryContext(Logging logging) {
        TelemetryContext telemetryContext = new TelemetryContext();
        LogShipperContext logShipperContext = LogShipperContext
                .builder()
                .enabled()
                .cloudStorageLogging()
                .withCloudRegion(REGION_SAMPLE)
                .withVmLogs(new ArrayList<>())
                .build();
        TelemetryClusterDetails clusterDetails = TelemetryClusterDetails.Builder.builder()
                .withCrn(DATAHUB_CRN)
                .build();
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        telemetryContext.setTelemetry(telemetry);
        telemetryContext.setLogShipperContext(logShipperContext);
        telemetryContext.setClusterDetails(clusterDetails);
        telemetryContext.setClusterType(FluentClusterType.DATAHUB);
        return telemetryContext;
    }
}