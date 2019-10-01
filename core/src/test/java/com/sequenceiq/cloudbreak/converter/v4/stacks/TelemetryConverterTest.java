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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
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

    private TelemetryConverter underTest;

    @Before
    public void setUp() {
        underTest = new TelemetryConverter(true, true, true, DATABUS_ENDPOINT);
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
        FeatureSetting reportDeploymentLogs = new FeatureSetting();
        reportDeploymentLogs.setEnabled(false);
        featuresRequest.setReportDeploymentLogs(reportDeploymentLogs);
        telemetryRequest.setLogging(logging);
        telemetryRequest.setFeatures(featuresRequest);
        telemetryRequest.setWorkloadAnalytics(workloadAnalyticsRequest);
        // WHEN
        Telemetry result = underTest.convert(telemetryRequest, StackType.WORKLOAD);
        // THEN
        assertNotNull(result.getFeatures().getWorkloadAnalytics());
        assertFalse(result.getFeatures().getReportDeploymentLogs().isEnabled());
        assertTrue(result.getFeatures().getMetering().isEnabled());
        assertTrue(result.getFeatures().getWorkloadAnalytics().isEnabled());
        assertEquals(DATABUS_ENDPOINT, result.getDatabusEndpoint());
        assertEquals(DATABUS_ENDPOINT, result.getWorkloadAnalytics().getDatabusEndpoint());
    }

    @Test
    public void testConvertToResponseWithEnabledReportDeploymentLogFeatures() {
        // GIVEN
        Logging logging = new Logging();
        S3CloudStorageV1Parameters s3Params = new S3CloudStorageV1Parameters();
        s3Params.setInstanceProfile(INSTANCE_PROFILE_VALUE);
        logging.setS3(s3Params);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        Features features = new Features();
        FeatureSetting reportDeploymentLogs = new FeatureSetting();
        reportDeploymentLogs.setEnabled(true);
        features.setWorkloadAnalytics(null);
        features.setReportDeploymentLogs(reportDeploymentLogs);
        telemetry.setFeatures(features);
        // WHEN
        TelemetryResponse result = underTest.convert(telemetry);
        // THEN
        assertEquals(INSTANCE_PROFILE_VALUE, result.getLogging().getS3().getInstanceProfile());
        assertTrue(result.getFeatures().getReportDeploymentLogs().isEnabled());
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
    public void testConvertFromRequestWithDefaultFeatures() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        // WHEN
        Telemetry result = underTest.convert(telemetryRequest, StackType.WORKLOAD);
        // THEN
        assertTrue(result.getFeatures().getReportDeploymentLogs().isEnabled());
        assertTrue(result.getFeatures().getMetering().isEnabled());
    }

    @Test
    public void testConvertFromRequestForDatalake() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        // WHEN
        Telemetry result = underTest.convert(telemetryRequest, StackType.DATALAKE);
        // THEN
        assertTrue(result.getFeatures().getReportDeploymentLogs().isEnabled());
        assertNull(result.getFeatures().getMetering());
    }

    @Test
    public void testConvertFromEnvAndSdxResponseWithoutInputs() {
        // GIVEN
        // WHEN
        TelemetryRequest result = underTest.convert(null, null, null);
        // THEN
        assertNotNull(result.getWorkloadAnalytics());
        assertNotNull(result.getFeatures());
        assertTrue(result.getFeatures().getWorkloadAnalytics().isEnabled());
        assertNull(result.getLogging());
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
        TelemetryRequest result = underTest.convert(response, sdxClusterResponse, null);
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
        FeatureSetting waFeatureSetting = new FeatureSetting();
        waFeatureSetting.setEnabled(true);
        // WHEN
        TelemetryRequest result = underTest.convert(response, sdxClusterResponse, waFeatureSetting);
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
        FeatureSetting waFeatureSetting = new FeatureSetting();
        waFeatureSetting.setEnabled(false);
        // WHEN
        TelemetryRequest result = underTest.convert(response, sdxClusterResponse, waFeatureSetting);
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
        TelemetryConverter converter = new TelemetryConverter(false, true, true, DATABUS_ENDPOINT);
        // WHEN
        TelemetryRequest result = converter.convert(response, sdxClusterResponse, null);
        // THEN
        assertNull(result.getWorkloadAnalytics());
    }

    @Test
    public void testConvertFromEnvAndSdxResponseWithReportDeploymentLogsEnabled() {
        // GIVEN
        TelemetryResponse response = new TelemetryResponse();
        FeatureSetting reportDeploymentLogs = new FeatureSetting();
        reportDeploymentLogs.setEnabled(true);
        FeaturesResponse featuresResponse = new FeaturesResponse();
        featuresResponse.setReportDeploymentLogs(reportDeploymentLogs);
        response.setFeatures(featuresResponse);
        // WHEN
        TelemetryRequest result = underTest.convert(response, null, null);
        // THEN
        assertTrue(result.getFeatures().getReportDeploymentLogs().isEnabled());
    }

    @Test
    public void testConvertFromEnvAndSdxResponseWithReportDeploymentLogsDisabled() {
        // GIVEN
        TelemetryResponse response = new TelemetryResponse();
        FeatureSetting reportDeploymentLogs = new FeatureSetting();
        reportDeploymentLogs.setEnabled(false);
        FeaturesResponse featuresResponse = new FeaturesResponse();
        featuresResponse.setReportDeploymentLogs(reportDeploymentLogs);
        response.setFeatures(featuresResponse);
        // WHEN
        TelemetryRequest result = underTest.convert(response, null, null);
        // THEN
        assertFalse(result.getFeatures().getReportDeploymentLogs().isEnabled());
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
        FeatureSetting reportDeploymentLogs = new FeatureSetting();
        reportDeploymentLogs.setEnabled(true);
        features.setReportDeploymentLogs(reportDeploymentLogs);
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
        assertTrue(result.getFeatures().getReportDeploymentLogs().isEnabled());
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
