package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfiguration;
import com.sequenceiq.cloudbreak.telemetry.logcollection.ClusterLogsCollectionConfiguration;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.WorkloadAnalytics;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.request.WorkloadAnalyticsRequest;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

public class TelemetryConverterTest {

    private static final String INSTANCE_PROFILE_VALUE = "myInstanceProfile";

    private static final String DATABUS_ENDPOINT = "myCustomEndpoint";

    private static final String DATABUS_S3_BUCKET = "myCustomS3Bucket";

    private TelemetryConverter underTest;

    @Before
    public void setUp() {
        AltusDatabusConfiguration altusDatabusConfiguration = new AltusDatabusConfiguration(DATABUS_ENDPOINT, DATABUS_S3_BUCKET, true, "****", "****");
        MeteringConfiguration meteringConfiguration = new MeteringConfiguration(true, "app", "stream");
        ClusterLogsCollectionConfiguration logCollectionConfig = new ClusterLogsCollectionConfiguration(true, "app", "stream");
        MonitoringConfiguration monitoringConfig = new MonitoringConfiguration(true, null, null);
        TelemetryConfiguration telemetryConfiguration =
                new TelemetryConfiguration(altusDatabusConfiguration, meteringConfiguration, logCollectionConfig, monitoringConfig, null);
        underTest = new TelemetryConverter(telemetryConfiguration, true, true);
    }

    @Test
    public void testConvertToResponse() {
        // GIVEN
        Logging logging = new Logging();
        S3CloudStorageV1Parameters s3Params = new S3CloudStorageV1Parameters();
        s3Params.setInstanceProfile(INSTANCE_PROFILE_VALUE);
        logging.setS3(s3Params);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        TelemetryResponse result = underTest.convert(telemetry);
        // THEN
        assertEquals(INSTANCE_PROFILE_VALUE, result.getLogging().getS3().getInstanceProfile());
    }

    @Test
    public void testConvertFromRequest() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        LoggingRequest logging = new LoggingRequest();
        logging.setS3(new S3CloudStorageV1Parameters());
        WorkloadAnalyticsRequest workloadAnalyticsRequest = new WorkloadAnalyticsRequest();
        FeaturesRequest featuresRequest = new FeaturesRequest();
        featuresRequest.addClusterLogsCollection(false);
        featuresRequest.addMonitoring(true);
        featuresRequest.addCloudStorageLogging(false);
        telemetryRequest.setLogging(logging);
        telemetryRequest.setFeatures(featuresRequest);
        telemetryRequest.setWorkloadAnalytics(workloadAnalyticsRequest);
        // WHEN
        Telemetry result = underTest.convert(telemetryRequest, StackType.WORKLOAD);
        // THEN
        assertNotNull(result.getFeatures().getWorkloadAnalytics());
        assertFalse(result.getFeatures().getClusterLogsCollection().isEnabled());
        assertFalse(result.getFeatures().getCloudStorageLogging().isEnabled());
        assertTrue(result.getFeatures().getMetering().isEnabled());
        assertTrue(result.getFeatures().getMonitoring().isEnabled());
        assertTrue(result.getFeatures().getWorkloadAnalytics().isEnabled());
        assertTrue(result.getFeatures().getUseSharedAltusCredential().isEnabled());
        assertEquals(DATABUS_ENDPOINT, result.getDatabusEndpoint());
        assertEquals(DATABUS_ENDPOINT, result.getWorkloadAnalytics().getDatabusEndpoint());
    }

    @Test
    public void testConvertToResponseWithEnabledClusterLogsCollectionFeatures() {
        // GIVEN
        Logging logging = new Logging();
        S3CloudStorageV1Parameters s3Params = new S3CloudStorageV1Parameters();
        s3Params.setInstanceProfile(INSTANCE_PROFILE_VALUE);
        logging.setS3(s3Params);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        Features features = new Features();
        features.setWorkloadAnalytics(null);
        features.addClusterLogsCollection(true);
        telemetry.setFeatures(features);
        // WHEN
        TelemetryResponse result = underTest.convert(telemetry);
        // THEN
        assertEquals(INSTANCE_PROFILE_VALUE, result.getLogging().getS3().getInstanceProfile());
        assertTrue(result.getFeatures().getClusterLogsCollection().isEnabled());
        assertNull(result.getFeatures().getWorkloadAnalytics());
        assertNull(result.getFeatures().getMetering());
    }

    @Test
    public void testConvertFromRequestWithAttributes() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        Map<String, Object> fluentAttributes = new HashMap<>();
        fluentAttributes.put("myAttrKey", "myAttrValue");
        telemetryRequest.setFluentAttributes(fluentAttributes);
        // WHEN
        Telemetry result = underTest.convert(telemetryRequest, StackType.WORKLOAD);
        // THEN
        assertEquals(1, result.getFluentAttributes().size());
    }

    @Test
    public void testConvertWhenWorkloadAnalyticsIsNotNullThenItsAttributesShouldBePassed() {
        WorkloadAnalytics workloadAnalytics = new WorkloadAnalytics();
        workloadAnalytics.setDatabusEndpoint(DATABUS_ENDPOINT);
        workloadAnalytics.setAttributes(Map.of("someAttributeKey", "someOtherStuffForValue"));
        Telemetry input = new Telemetry();
        input.setWorkloadAnalytics(workloadAnalytics);

        TelemetryResponse response = underTest.convert(input);

        assertNotNull(response);
        assertNotNull(response.getWorkloadAnalytics());
        assertEquals(input.getWorkloadAnalytics().getAttributes(), response.getWorkloadAnalytics().getAttributes());
    }

    @Test
    public void testConvertWhenMonitoringIsDisabledThenItShouldBeFalseInTheResult() {
        ReflectionTestUtils.setField(underTest, "monitoringEnabled", false);
        TelemetryRequest input = new TelemetryRequest();

        Telemetry result = underTest.convert(input, StackType.WORKLOAD);

        assertNotNull(result);
        assertFalse(result.getFeatures().getMonitoring().isEnabled());
    }

    @Test
    public void testConvertWhenClusterLogCollectionIsDisabledThenItShouldBeFalseInTheResult() {
        ReflectionTestUtils.setField(underTest, "clusterLogsCollection", false);
        TelemetryRequest input = new TelemetryRequest();

        Telemetry result = underTest.convert(input, StackType.WORKLOAD);

        assertNotNull(result);
        assertFalse(result.getFeatures().getClusterLogsCollection().isEnabled());
    }

    @Test
    public void testConvertFromRequestWithDefaultFeatures() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        // WHEN
        Telemetry result = underTest.convert(telemetryRequest, StackType.WORKLOAD);
        // THEN
        assertFalse(result.getFeatures().getClusterLogsCollection().isEnabled());
        assertTrue(result.getFeatures().getMetering().isEnabled());
        assertTrue(result.getFeatures().getCloudStorageLogging().isEnabled());
    }

    @Test
    public void testConvertFromRequestWithFeatures() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        FeaturesRequest features = new FeaturesRequest();
        features.addClusterLogsCollection(true);
        telemetryRequest.setFeatures(features);
        // WHEN
        Telemetry result = underTest.convert(telemetryRequest, StackType.WORKLOAD);
        // THEN
        assertTrue(result.getFeatures().getClusterLogsCollection().isEnabled());
        assertTrue(result.getFeatures().getMetering().isEnabled());
    }

    @Test
    public void testConvertFromRequestForDatalake() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        // WHEN
        Telemetry result = underTest.convert(telemetryRequest, StackType.DATALAKE);
        // THEN
        assertFalse(result.getFeatures().getClusterLogsCollection().isEnabled());
        assertNull(result.getFeatures().getMetering());
    }

    @Test
    public void testConvertFromEnvAndSdxResponseWithoutInputs() {
        // GIVEN
        SdxClusterResponse sdxClusterResponse = null;
        // WHEN
        TelemetryRequest result = underTest.convert(null, sdxClusterResponse);
        // THEN
        assertNotNull(result.getWorkloadAnalytics());
        assertNotNull(result.getFeatures());
        assertTrue(result.getFeatures().getWorkloadAnalytics().isEnabled());
        assertNull(result.getLogging());
    }

    @Test
    public void testConvertFromEnvAndSdxResponseWithDefaultDisabled() {
        // GIVEN
        SdxClusterResponse sdxClusterResponse = null;
        AltusDatabusConfiguration altusDatabusConfiguration = new AltusDatabusConfiguration(DATABUS_ENDPOINT, DATABUS_S3_BUCKET, false, "", null);
        MeteringConfiguration meteringConfiguration = new MeteringConfiguration(true, null, null);
        ClusterLogsCollectionConfiguration logCollectionConfig = new ClusterLogsCollectionConfiguration(true, null, null);
        MonitoringConfiguration monitoringConfig = new MonitoringConfiguration(false, null, null);
        TelemetryConfiguration telemetryConfiguration =
                new TelemetryConfiguration(altusDatabusConfiguration, meteringConfiguration, logCollectionConfig, monitoringConfig, null);
        TelemetryConverter converter = new TelemetryConverter(telemetryConfiguration, true, false);
        // WHEN
        TelemetryRequest result = converter.convert(null, sdxClusterResponse);
        // THEN
        assertNull(result.getWorkloadAnalytics());
        assertNull(result.getFeatures().getMonitoring());
        assertNotNull(result.getFeatures());
        assertFalse(result.getFeatures().getWorkloadAnalytics().isEnabled());
    }

    @Test
    public void testConvertFromEnvAndSdxResponseWithoutWAInput() {
        // GIVEN
        TelemetryResponse response = new TelemetryResponse();
        LoggingResponse loggingResponse = new LoggingResponse();
        S3CloudStorageV1Parameters s3Params = new S3CloudStorageV1Parameters();
        s3Params.setInstanceProfile(INSTANCE_PROFILE_VALUE);
        loggingResponse.setS3(s3Params);
        response.setLogging(loggingResponse);
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setCrn("crn:cdp:cloudbreak:us-west-1:someone:sdxcluster:sdxId");
        sdxClusterResponse.setName("sdxName");
        // WHEN
        TelemetryRequest result = underTest.convert(response, sdxClusterResponse);
        // THEN
        assertTrue(result.getFeatures().getWorkloadAnalytics().isEnabled());
        assertEquals(INSTANCE_PROFILE_VALUE, result.getLogging().getS3().getInstanceProfile());
        assertEquals("sdxId", result.getWorkloadAnalytics().getAttributes().get("databus.header.sdx.id").toString());
        assertEquals("sdxName", result.getWorkloadAnalytics().getAttributes().get("databus.header.sdx.name").toString());
    }

    @Test
    public void testConvertFromEnvAndSdxResponseWithWAEnabled() {
        // GIVEN
        TelemetryResponse response = new TelemetryResponse();
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setCrn("crn:cdp:cloudbreak:us-west-1:someone:sdxcluster:sdxId");
        sdxClusterResponse.setName("sdxName");
        // WHEN
        TelemetryRequest result = underTest.convert(response, sdxClusterResponse);
        // THEN
        assertTrue(result.getFeatures().getWorkloadAnalytics().isEnabled());
        assertEquals("sdxId", result.getWorkloadAnalytics().getAttributes().get("databus.header.sdx.id").toString());
        assertEquals("sdxName", result.getWorkloadAnalytics().getAttributes().get("databus.header.sdx.name").toString());
    }

    @Test
    public void testConvertFromEnvAndSdxResponseWithWADisabled() {
        // GIVEN
        TelemetryResponse response = new TelemetryResponse();
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setCrn("crn:cdp:cloudbreak:us-west-1:someone:sdxcluster:sdxId");
        sdxClusterResponse.setName("sdxName");
        FeaturesResponse featuresResponse = new FeaturesResponse();
        featuresResponse.addWorkloadAnalytics(false);
        response.setFeatures(featuresResponse);
        // WHEN
        TelemetryRequest result = underTest.convert(response, sdxClusterResponse);
        // THEN
        assertNull(result.getWorkloadAnalytics());
        assertFalse(result.getFeatures().getWorkloadAnalytics().isEnabled());
    }

    @Test
    public void testConvertFromEnvAndSdxResponseWithWADisabledGlobally() {
        // GIVEN
        TelemetryResponse response = new TelemetryResponse();
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setCrn("crn:cdp:cloudbreak:us-west-1:someone:sdxcluster:sdxId");
        sdxClusterResponse.setName("sdxName");
        AltusDatabusConfiguration altusDatabusConfiguration = new AltusDatabusConfiguration(DATABUS_ENDPOINT, DATABUS_S3_BUCKET, false, "", null);
        MeteringConfiguration meteringConfiguration = new MeteringConfiguration(true, null, null);
        ClusterLogsCollectionConfiguration logCollectionConfig = new ClusterLogsCollectionConfiguration(true, null, null);
        MonitoringConfiguration monitoringConfig = new MonitoringConfiguration(false, null, null);
        TelemetryConfiguration telemetryConfiguration =
                new TelemetryConfiguration(altusDatabusConfiguration, meteringConfiguration, logCollectionConfig, monitoringConfig, null);
        TelemetryConverter converter = new TelemetryConverter(telemetryConfiguration, false, true);
        // WHEN
        TelemetryRequest result = converter.convert(response, sdxClusterResponse);
        // THEN
        assertNull(result.getWorkloadAnalytics());
    }

    @Test
    public void testConvertFromEnvAndSdxResponseWithClusterLogsCollectionEnabled() {
        // GIVEN
        TelemetryResponse response = new TelemetryResponse();
        FeaturesResponse featuresResponse = new FeaturesResponse();
        featuresResponse.addClusterLogsCollection(true);
        response.setFeatures(featuresResponse);
        // WHEN
        TelemetryRequest result = underTest.convert(response, null);
        // THEN
        assertTrue(result.getFeatures().getClusterLogsCollection().isEnabled());
    }

    @Test
    public void testConvertWithCloudStorageLoggingNotEnabled() {
        // GIVEN
        TelemetryResponse response = new TelemetryResponse();
        FeaturesResponse featuresResponse = new FeaturesResponse();
        FeatureSetting fs = new FeatureSetting();
        fs.setEnabled(false);
        featuresResponse.setCloudStorageLogging(fs);
        response.setFeatures(featuresResponse);
        // WHEN
        TelemetryRequest result = underTest.convert(response, null);
        // THEN
        assertFalse(result.getFeatures().getCloudStorageLogging().isEnabled());
    }

    @Test
    public void testConvertFromEnvAndSdxResponseWithClusterLogsCollectionDisabled() {
        // GIVEN
        TelemetryResponse response = new TelemetryResponse();
        FeaturesResponse featuresResponse = new FeaturesResponse();
        featuresResponse.addClusterLogsCollection(false);
        response.setFeatures(featuresResponse);
        // WHEN
        TelemetryRequest result = underTest.convert(response, null);
        // THEN
        assertFalse(result.getFeatures().getClusterLogsCollection().isEnabled());
    }

    @Test
    public void testConvertToRequest() {
        // GIVEN
        Telemetry telemetry = new Telemetry();
        telemetry.setDatabusEndpoint(DATABUS_ENDPOINT);
        Logging logging = new Logging();
        logging.setS3(new S3CloudStorageV1Parameters());
        telemetry.setLogging(logging);
        Features features = new Features();
        features.addClusterLogsCollection(true);
        telemetry.setFeatures(features);
        WorkloadAnalytics workloadAnalytics = new WorkloadAnalytics();
        Map<String, Object> waAttributes = new HashMap<>();
        waAttributes.put("myWAKey", "myWAValue");
        workloadAnalytics.setAttributes(waAttributes);
        telemetry.setWorkloadAnalytics(workloadAnalytics);
        Map<String, Object> fluentAttributes = new HashMap<>();
        fluentAttributes.put("myKey", "myValue");
        telemetry.setFluentAttributes(fluentAttributes);
        // WHEN
        TelemetryRequest result = underTest.convertToRequest(telemetry);
        // THEN
        assertNotNull(result.getLogging().getS3());
        assertEquals("myValue", result.getFluentAttributes().get("myKey"));
        assertEquals("myWAValue", result.getWorkloadAnalytics().getAttributes().get("myWAKey"));
        assertTrue(result.getFeatures().getClusterLogsCollection().isEnabled());
    }

    @Test
    public void testConvertToRequestWithEmptyTelemetry() {
        // GIVEN
        Telemetry telemetry = new Telemetry();
        // WHEN
        TelemetryRequest result = underTest.convertToRequest(telemetry);
        // THEN
        assertNotNull(result);
        assertNull(result.getLogging());
        assertNull(result.getWorkloadAnalytics());
        assertNull(result.getFeatures());
    }

}