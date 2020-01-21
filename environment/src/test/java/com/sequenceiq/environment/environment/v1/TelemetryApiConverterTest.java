package com.sequenceiq.environment.environment.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfiguration;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.request.WorkloadAnalyticsRequest;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.dto.telemetry.S3CloudStorageParameters;

public class TelemetryApiConverterTest {

    private static final String INSTANCE_PROFILE_VALUE = "myInstanceProfile";

    private TelemetryApiConverter underTest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        AltusDatabusConfiguration altusDatabusConfiguration = new AltusDatabusConfiguration("", true, "****", "****");
        TelemetryConfiguration telemetryConfiguration = new TelemetryConfiguration(altusDatabusConfiguration, true, true);
        underTest = new TelemetryApiConverter(telemetryConfiguration);
    }

    @Test
    public void testConvert() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        LoggingRequest loggingRequest = new LoggingRequest();
        S3CloudStorageV1Parameters s3Params = new S3CloudStorageV1Parameters();
        s3Params.setInstanceProfile(INSTANCE_PROFILE_VALUE);
        loggingRequest.setS3(s3Params);
        telemetryRequest.setLogging(loggingRequest);
        telemetryRequest.setWorkloadAnalytics(new WorkloadAnalyticsRequest());
        FeaturesRequest fr = new FeaturesRequest();
        FeatureSetting fsReportLogs = new FeatureSetting();
        fsReportLogs.setEnabled(true);
        FeatureSetting fsWorladAnalytics = new FeatureSetting();
        fsWorladAnalytics.setEnabled(true);
        FeatureSetting fsUseSharedCredential = new FeatureSetting();
        fsUseSharedCredential.setEnabled(true);
        fr.setReportDeploymentLogs(fsReportLogs);
        fr.setWorkloadAnalytics(fsWorladAnalytics);
        fr.setUseSharedAltusCredential(fsUseSharedCredential);
        telemetryRequest.setFeatures(fr);
        // WHEN
        EnvironmentTelemetry result = underTest.convert(telemetryRequest);
        // THEN
        assertEquals(INSTANCE_PROFILE_VALUE, result.getLogging().getS3().getInstanceProfile());
        assertTrue(result.getFeatures().getReportDeploymentLogs().isEnabled());
        assertTrue(result.getFeatures().getWorkloadAnalytics().isEnabled());
        assertTrue(result.getFeatures().getUseSharedAltusCredential().isEnabled());
    }

    @Test
    public void testConvertWithDefaults() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        // WHEN
        EnvironmentTelemetry result = underTest.convert(telemetryRequest);
        // THEN
        assertNull(result.getFeatures());
    }

    @Test
    public void testConvertWithDefaultFeatures() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        FeaturesRequest fr = new FeaturesRequest();
        telemetryRequest.setFeatures(fr);
        // WHEN
        EnvironmentTelemetry result = underTest.convert(telemetryRequest);
        // THEN
        assertNull(result.getFeatures().getWorkloadAnalytics());
    }

    @Test
    public void testConvertWithWAFeature() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        FeaturesRequest fr = new FeaturesRequest();
        FeatureSetting waFeature = new FeatureSetting();
        waFeature.setEnabled(true);
        fr.setWorkloadAnalytics(waFeature);
        telemetryRequest.setFeatures(fr);
        // WHEN
        EnvironmentTelemetry result = underTest.convert(telemetryRequest);
        // THEN
        assertTrue(result.getFeatures().getWorkloadAnalytics().isEnabled());
    }

    @Test
    public void testConvertWithWADisabled() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        FeaturesRequest fr = new FeaturesRequest();
        FeatureSetting waFeature = new FeatureSetting();
        waFeature.setEnabled(false);
        fr.setWorkloadAnalytics(waFeature);
        telemetryRequest.setFeatures(fr);
        // WHEN
        EnvironmentTelemetry result = underTest.convert(telemetryRequest);
        // THEN
        assertFalse(result.getFeatures().getWorkloadAnalytics().isEnabled());
    }

    @Test
    public void testConvertToResponse() {
        // GIVEN
        EnvironmentLogging logging = new EnvironmentLogging();
        S3CloudStorageParameters s3Params = new S3CloudStorageParameters();
        s3Params.setInstanceProfile(INSTANCE_PROFILE_VALUE);
        logging.setS3(s3Params);
        EnvironmentTelemetry telemetry = new EnvironmentTelemetry();
        telemetry.setLogging(logging);
        // WHEN
        TelemetryResponse result = underTest.convert(telemetry);
        // THEN
        assertEquals(INSTANCE_PROFILE_VALUE, result.getLogging().getS3().getInstanceProfile());
        assertNull(result.getWorkloadAnalytics());
    }

    @Test
    public void testConvertToRequest() {
        // GIVEN
        EnvironmentLogging logging = new EnvironmentLogging();
        S3CloudStorageParameters s3Params = new S3CloudStorageParameters();
        s3Params.setInstanceProfile(INSTANCE_PROFILE_VALUE);
        logging.setS3(s3Params);
        EnvironmentTelemetry telemetry = new EnvironmentTelemetry();
        telemetry.setLogging(logging);
        // WHEN
        TelemetryRequest result = underTest.convertToRequest(telemetry);
        // THEN
        assertNull(result.getFeatures());
        assertEquals(INSTANCE_PROFILE_VALUE, result.getLogging().getS3().getInstanceProfile());
    }

    @Test
    public void testConvertToRequestWithFeatures() {
        // GIVEN
        EnvironmentLogging logging = new EnvironmentLogging();
        S3CloudStorageParameters s3Params = new S3CloudStorageParameters();
        s3Params.setInstanceProfile(INSTANCE_PROFILE_VALUE);
        logging.setS3(s3Params);
        EnvironmentFeatures features = new EnvironmentFeatures();
        FeatureSetting reportDeploymentLogs = new FeatureSetting();
        reportDeploymentLogs.setEnabled(false);
        features.setReportDeploymentLogs(reportDeploymentLogs);
        EnvironmentTelemetry telemetry = new EnvironmentTelemetry();
        telemetry.setLogging(logging);
        telemetry.setFeatures(features);
        // WHEN
        TelemetryRequest result = underTest.convertToRequest(telemetry);
        // THEN
        assertNotNull(result.getFeatures());
        assertFalse(result.getFeatures().getReportDeploymentLogs().isEnabled());
        assertEquals(INSTANCE_PROFILE_VALUE, result.getLogging().getS3().getInstanceProfile());
    }

}