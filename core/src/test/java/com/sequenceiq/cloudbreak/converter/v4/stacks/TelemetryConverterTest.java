package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringUrlResolver;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Monitoring;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.WorkloadAnalytics;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.MonitoringRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.request.WorkloadAnalyticsRequest;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.MonitoringResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@ExtendWith(MockitoExtension.class)
class TelemetryConverterTest {

    private static final String INSTANCE_PROFILE_VALUE = "myInstanceProfile";

    private static final String DATABUS_ENDPOINT = "myCustomEndpoint";

    private static final String DATABUS_S3_BUCKET = "myCustomS3Bucket";

    private static final String MONITORING_REMOTE_WRITE_URL = "http://myendpoint/api/v1/write";

    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private MonitoringUrlResolver monitoringUrlResolver;

    private TelemetryConverter underTest;

    @BeforeEach
    public void setUp() {
        AltusDatabusConfiguration altusDatabusConfiguration = new AltusDatabusConfiguration(DATABUS_ENDPOINT, DATABUS_S3_BUCKET, true, "****", "****");
        MonitoringConfiguration monitoringConfig = new MonitoringConfiguration();
        lenient().when(entitlementService.isComputeMonitoringEnabled(anyString())).thenReturn(true);
        TelemetryConfiguration telemetryConfiguration =
                new TelemetryConfiguration(altusDatabusConfiguration, monitoringConfig, null);
        underTest = new TelemetryConverter(telemetryConfiguration, entitlementService, true, true, monitoringUrlResolver);
    }

    @Test
    void testConvertToResponse() {
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
    void testConvertFromRequest() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        LoggingRequest logging = new LoggingRequest();
        logging.setS3(new S3CloudStorageV1Parameters());
        WorkloadAnalyticsRequest workloadAnalyticsRequest = new WorkloadAnalyticsRequest();
        FeaturesRequest featuresRequest = new FeaturesRequest();
        featuresRequest.addMonitoring(true);
        featuresRequest.addCloudStorageLogging(false);
        telemetryRequest.setLogging(logging);
        telemetryRequest.setFeatures(featuresRequest);
        telemetryRequest.setWorkloadAnalytics(workloadAnalyticsRequest);
        // WHEN
        Telemetry result = underTest.convert(telemetryRequest, StackType.WORKLOAD, ACCOUNT_ID);
        // THEN
        assertNotNull(result.getFeatures().getWorkloadAnalytics());
        assertFalse(result.getFeatures().getCloudStorageLogging().getEnabled());
        assertTrue(result.getFeatures().getMonitoring().getEnabled());
        assertTrue(result.getFeatures().getWorkloadAnalytics().getEnabled());
        assertTrue(result.getFeatures().getUseSharedAltusCredential().getEnabled());
        assertEquals(DATABUS_ENDPOINT, result.getDatabusEndpoint());
        assertEquals(DATABUS_ENDPOINT, result.getWorkloadAnalytics().getDatabusEndpoint());
    }

    @Test
    void testConvertToResponseWithEnabledFeatures() {
        // GIVEN
        Logging logging = new Logging();
        S3CloudStorageV1Parameters s3Params = new S3CloudStorageV1Parameters();
        s3Params.setInstanceProfile(INSTANCE_PROFILE_VALUE);
        logging.setS3(s3Params);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        Features features = new Features();
        features.setWorkloadAnalytics(null);
        telemetry.setFeatures(features);
        // WHEN
        TelemetryResponse result = underTest.convert(telemetry);
        // THEN
        assertEquals(INSTANCE_PROFILE_VALUE, result.getLogging().getS3().getInstanceProfile());
        assertNull(result.getFeatures().getWorkloadAnalytics());
    }

    @Test
    void testConvertFromRequestWithAttributes() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        Map<String, Object> fluentAttributes = new HashMap<>();
        fluentAttributes.put("myAttrKey", "myAttrValue");
        telemetryRequest.setFluentAttributes(fluentAttributes);
        // WHEN
        Telemetry result = underTest.convert(telemetryRequest, StackType.WORKLOAD, ACCOUNT_ID);
        // THEN
        assertEquals(1, result.getFluentAttributes().size());
    }

    @Test
    void testConvertWhenWorkloadAnalyticsIsNotNullThenItsAttributesShouldBePassed() {
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
    void testConvertWhenMonitoringIsDisabledThenItShouldBeFalseInTheResult() {
        when(entitlementService.isComputeMonitoringEnabled(anyString())).thenReturn(false);
        TelemetryRequest input = new TelemetryRequest();

        Telemetry result = underTest.convert(input, StackType.WORKLOAD, ACCOUNT_ID);

        assertNotNull(result);
        assertFalse(result.getFeatures().getMonitoring().getEnabled());
    }

    @Test
    void testConvertWhenMonitoringIsDisabledButUrlIsInRequest() {
        when(entitlementService.isComputeMonitoringEnabled(anyString())).thenReturn(false);
        TelemetryRequest input = new TelemetryRequest();
        MonitoringRequest monitoring = new MonitoringRequest();
        monitoring.setRemoteWriteUrl("anurl");
        input.setMonitoring(monitoring);

        Telemetry result = underTest.convert(input, StackType.WORKLOAD, ACCOUNT_ID);

        assertNotNull(result);
        assertFalse(result.getFeatures().getMonitoring().getEnabled());
    }

    @Test
    void testConvertFromRequestWithDefaultFeatures() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        // WHEN
        Telemetry result = underTest.convert(telemetryRequest, StackType.WORKLOAD, ACCOUNT_ID);
        // THEN
        assertTrue(result.getFeatures().getCloudStorageLogging().getEnabled());
    }

    @Test
    void testConvertFromEnvAndSdxResponseWithoutInputs() {
        // GIVEN
        SdxClusterResponse sdxClusterResponse = null;
        // WHEN
        TelemetryRequest result = underTest.convert(new DetailedEnvironmentResponse(), null, sdxClusterResponse);
        // THEN
        assertNotNull(result.getWorkloadAnalytics());
        assertNotNull(result.getFeatures());
        assertTrue(result.getFeatures().getWorkloadAnalytics().getEnabled());
        assertNull(result.getLogging());
    }

    @Test
    void testConvertFromEnvAndSdxResponseWithDefaultDisabled() {
        // GIVEN
        SdxClusterResponse sdxClusterResponse = null;
        AltusDatabusConfiguration altusDatabusConfiguration = new AltusDatabusConfiguration(DATABUS_ENDPOINT, DATABUS_S3_BUCKET, false, "", null);
        MonitoringConfiguration monitoringConfig = new MonitoringConfiguration();
        TelemetryConfiguration telemetryConfiguration =
                new TelemetryConfiguration(altusDatabusConfiguration, monitoringConfig, null);
        TelemetryConverter converter = new TelemetryConverter(telemetryConfiguration, entitlementService, true, false, monitoringUrlResolver);
        // WHEN
        TelemetryRequest result = converter.convert(new DetailedEnvironmentResponse(), null, sdxClusterResponse);
        // THEN
        assertNull(result.getWorkloadAnalytics());
        assertNull(result.getFeatures().getMonitoring());
        assertNotNull(result.getFeatures());
        assertFalse(result.getFeatures().getWorkloadAnalytics().getEnabled());
    }

    @Test
    void testConvertFromEnvAndSdxResponseWithoutWAInput() {
        // GIVEN
        TelemetryResponse response = new TelemetryResponse();
        LoggingResponse loggingResponse = new LoggingResponse();
        S3CloudStorageV1Parameters s3Params = new S3CloudStorageV1Parameters();
        s3Params.setInstanceProfile(INSTANCE_PROFILE_VALUE);
        loggingResponse.setS3(s3Params);
        response.setLogging(loggingResponse);
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        String sdxCrn = "crn:cdp:cloudbreak:us-west-1:someone:sdxcluster:sdxId";
        sdxClusterResponse.setCrn(sdxCrn);
        sdxClusterResponse.setName("sdxName");
        sdxClusterResponse.setEnvironmentCrn("envCrn");
        sdxClusterResponse.setEnvironmentName("envName");
        // WHEN
        TelemetryRequest result = underTest.convert(new DetailedEnvironmentResponse(), response, sdxClusterResponse);
        // THEN
        assertTrue(result.getFeatures().getWorkloadAnalytics().getEnabled());
        assertEquals(INSTANCE_PROFILE_VALUE, result.getLogging().getS3().getInstanceProfile());
        assertEquals("sdxId", result.getWorkloadAnalytics().getAttributes().get("databus.header.sdx.id").toString());
        assertEquals("sdxName", result.getWorkloadAnalytics().getAttributes().get("databus.header.sdx.name").toString());
        assertEquals("envCrn", result.getWorkloadAnalytics().getAttributes().get("databus.header.environment.crn").toString());
        assertEquals("envName", result.getWorkloadAnalytics().getAttributes().get("databus.header.environment.name").toString());
        assertEquals("sdxName", result.getWorkloadAnalytics().getAttributes().get("databus.header.datalake.name").toString());
        assertEquals(sdxCrn, result.getWorkloadAnalytics().getAttributes().get("databus.header.datalake.crn").toString());
    }

    @Test
    void testConvertFromEnvAndSdxResponseWithWAEnabled() {
        // GIVEN
        TelemetryResponse response = new TelemetryResponse();
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        String sdxCrn = "crn:cdp:cloudbreak:us-west-1:someone:sdxcluster:sdxId";
        sdxClusterResponse.setCrn(sdxCrn);
        sdxClusterResponse.setName("sdxName");
        sdxClusterResponse.setEnvironmentCrn("envCrn");
        sdxClusterResponse.setEnvironmentName("envName");
        // WHEN
        TelemetryRequest result = underTest.convert(new DetailedEnvironmentResponse(), response, sdxClusterResponse);
        // THEN
        assertTrue(result.getFeatures().getWorkloadAnalytics().getEnabled());
        assertEquals("sdxId", result.getWorkloadAnalytics().getAttributes().get("databus.header.sdx.id").toString());
        assertEquals("sdxName", result.getWorkloadAnalytics().getAttributes().get("databus.header.sdx.name").toString());
        assertEquals("envCrn", result.getWorkloadAnalytics().getAttributes().get("databus.header.environment.crn").toString());
        assertEquals("envName", result.getWorkloadAnalytics().getAttributes().get("databus.header.environment.name").toString());
        assertEquals("sdxName", result.getWorkloadAnalytics().getAttributes().get("databus.header.datalake.name").toString());
        assertEquals(sdxCrn, result.getWorkloadAnalytics().getAttributes().get("databus.header.datalake.crn").toString());
    }

    @Test
    void testConvertFromEnvAndSdxResponseWithWADisabled() {
        // GIVEN
        TelemetryResponse response = new TelemetryResponse();
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setCrn("crn:cdp:cloudbreak:us-west-1:someone:sdxcluster:sdxId");
        sdxClusterResponse.setName("sdxName");
        sdxClusterResponse.setEnvironmentCrn("envCrn");
        sdxClusterResponse.setEnvironmentName("envName");
        FeaturesResponse featuresResponse = new FeaturesResponse();
        featuresResponse.addWorkloadAnalytics(false);
        response.setFeatures(featuresResponse);
        // WHEN
        TelemetryRequest result = underTest.convert(new DetailedEnvironmentResponse(), response, sdxClusterResponse);
        // THEN
        assertNull(result.getWorkloadAnalytics());
        assertFalse(result.getFeatures().getWorkloadAnalytics().getEnabled());
    }

    @Test
    void testConvertFromEnvAndSdxResponseWithWADisabledGlobally() {
        // GIVEN
        TelemetryResponse response = new TelemetryResponse();
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setCrn("crn:cdp:cloudbreak:us-west-1:someone:sdxcluster:sdxId");
        sdxClusterResponse.setName("sdxName");
        sdxClusterResponse.setEnvironmentCrn("envCrn");
        sdxClusterResponse.setEnvironmentName("envName");
        AltusDatabusConfiguration altusDatabusConfiguration = new AltusDatabusConfiguration(DATABUS_ENDPOINT, DATABUS_S3_BUCKET, false, "", null);
        MonitoringConfiguration monitoringConfig = new MonitoringConfiguration();
        TelemetryConfiguration telemetryConfiguration =
                new TelemetryConfiguration(altusDatabusConfiguration, monitoringConfig, null);
        TelemetryConverter converter = new TelemetryConverter(telemetryConfiguration, entitlementService, false, true, monitoringUrlResolver);
        // WHEN
        TelemetryRequest result = converter.convert(new DetailedEnvironmentResponse(), response, sdxClusterResponse);
        // THEN
        assertNull(result.getWorkloadAnalytics());
    }

    @Test
    void testConvertFromEnvAndSdxResponseWithMonitoring() {
        // GIVEN
        TelemetryResponse response = new TelemetryResponse();
        FeaturesResponse featuresResponse = new FeaturesResponse();
        featuresResponse.addMonitoring(true);
        response.setFeatures(featuresResponse);
        MonitoringResponse monitoringResponse = new MonitoringResponse();
        monitoringResponse.setRemoteWriteUrl(MONITORING_REMOTE_WRITE_URL);
        response.setMonitoring(monitoringResponse);
        // WHEN
        TelemetryRequest result = underTest.convert(new DetailedEnvironmentResponse(), response, null);
        // THEN
        assertTrue(result.getFeatures().getMonitoring().getEnabled());
        assertEquals(MONITORING_REMOTE_WRITE_URL, result.getMonitoring().getRemoteWriteUrl());
    }

    @Test
    void testConvertWithCloudStorageLoggingNotEnabled() {
        // GIVEN
        TelemetryResponse response = new TelemetryResponse();
        FeaturesResponse featuresResponse = new FeaturesResponse();
        FeatureSetting fs = new FeatureSetting();
        fs.setEnabled(false);
        featuresResponse.setCloudStorageLogging(fs);
        response.setFeatures(featuresResponse);
        // WHEN
        TelemetryRequest result = underTest.convert(new DetailedEnvironmentResponse(), response, null);
        // THEN
        assertFalse(result.getFeatures().getCloudStorageLogging().getEnabled());
    }

    @Test
    void testConvertToRequest() {
        // GIVEN
        Telemetry telemetry = new Telemetry();
        telemetry.setDatabusEndpoint(DATABUS_ENDPOINT);
        Logging logging = new Logging();
        logging.setS3(new S3CloudStorageV1Parameters());
        telemetry.setLogging(logging);
        Monitoring monitoring = new Monitoring();
        monitoring.setRemoteWriteUrl(MONITORING_REMOTE_WRITE_URL);
        telemetry.setMonitoring(monitoring);
        Features features = new Features();
        features.addMonitoring(true);
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
        assertTrue(result.getFeatures().getMonitoring().getEnabled());
        assertEquals(MONITORING_REMOTE_WRITE_URL, result.getMonitoring().getRemoteWriteUrl());
    }

    @Test
    void testConvertToRequestWithEmptyTelemetry() {
        // GIVEN
        Telemetry telemetry = new Telemetry();
        // WHEN
        TelemetryRequest result = underTest.convertToRequest(telemetry);
        // THEN
        assertNotNull(result);
        assertNull(result.getLogging());
        assertNull(result.getMonitoring());
        assertNull(result.getWorkloadAnalytics());
        assertNull(result.getFeatures());
    }

}
