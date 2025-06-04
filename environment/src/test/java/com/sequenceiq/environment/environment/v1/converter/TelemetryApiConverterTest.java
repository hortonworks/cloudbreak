package com.sequenceiq.environment.environment.v1.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringUrlResolver;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.request.WorkloadAnalyticsRequest;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.dto.telemetry.S3CloudStorageParameters;
import com.sequenceiq.environment.telemetry.domain.AccountTelemetry;

@ExtendWith(MockitoExtension.class)
public class TelemetryApiConverterTest {

    private static final String INSTANCE_PROFILE_VALUE = "myInstanceProfile";

    private static final String ACCOUNT_ID = "accId";

    private static final String STORAGE_LOCATION = "storageLocation";

    private TelemetryApiConverter underTest;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StorageLocationDecorator storageLocationDecorator;

    @BeforeEach
    public void setUp() {
        AltusDatabusConfiguration altusDatabusConfiguration = new AltusDatabusConfiguration("", "", true, "****", "****");
        MonitoringConfiguration monitoringConfig = new MonitoringConfiguration();
        monitoringConfig.setRemoteWriteUrl("http://myaddress/api/v1/receive");
        TelemetryConfiguration telemetryConfiguration = new TelemetryConfiguration(
                altusDatabusConfiguration, monitoringConfig, null);
        underTest = new TelemetryApiConverter(telemetryConfiguration, new MonitoringUrlResolver(monitoringConfig), entitlementService, storageLocationDecorator);
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
        fr.addWorkloadAnalytics(true);
        fr.addMonitoring(true);
        telemetryRequest.setFeatures(fr);
        given(entitlementService.isComputeMonitoringEnabled(anyString())).willReturn(true);
        // WHEN
        EnvironmentTelemetry result = underTest.convert(telemetryRequest, getAccountTelemetry(), ACCOUNT_ID);
        // THEN
        assertEquals(INSTANCE_PROFILE_VALUE, result.getLogging().getS3().getInstanceProfile());
        assertEquals("http://myaddress/api/v1/receive", result.getMonitoring().getRemoteWriteUrl());
        assertTrue(result.getFeatures().getWorkloadAnalytics().getEnabled());
        assertTrue(result.getFeatures().getUseSharedAltusCredential().getEnabled());
        assertTrue(result.getFeatures().getMonitoring().getEnabled());
        assertTrue(result.getFeatures().getCloudStorageLogging().getEnabled());
    }

    @Test
    public void testConvertWithDefaults() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        // WHEN
        EnvironmentTelemetry result = underTest.convert(telemetryRequest, getAccountTelemetry(), ACCOUNT_ID);
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
        EnvironmentTelemetry result = underTest.convert(telemetryRequest, getAccountTelemetry(), ACCOUNT_ID);
        // THEN
        assertNull(result.getFeatures().getWorkloadAnalytics());
    }

    @Test
    public void testConvertWithWAFeature() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        FeaturesRequest fr = new FeaturesRequest();
        fr.addWorkloadAnalytics(true);
        telemetryRequest.setFeatures(fr);
        // WHEN
        EnvironmentTelemetry result = underTest.convert(telemetryRequest, getAccountTelemetry(), ACCOUNT_ID);
        // THEN
        assertTrue(result.getFeatures().getWorkloadAnalytics().getEnabled());
    }

    @Test
    public void testConvertWithDefaultMonitoringFeatureWithoutCdpSaas() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        // WHEN
        EnvironmentTelemetry result = underTest.convert(telemetryRequest, getAccountTelemetry(), ACCOUNT_ID);
        // THEN
        assertNull(result.getFeatures());
        assertNull(result.getMonitoring().getRemoteWriteUrl());
    }

    @Test
    public void testConvertWithMonitoringFeatureWithoutCdpSaas() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        FeaturesRequest featuresRequest = new FeaturesRequest();
        featuresRequest.addMonitoring(true);
        telemetryRequest.setFeatures(featuresRequest);
        given(entitlementService.isComputeMonitoringEnabled(anyString())).willReturn(false);
        // WHEN
        EnvironmentTelemetry result = underTest.convert(telemetryRequest, getAccountTelemetry(), ACCOUNT_ID);
        // THEN
        assertNull(result.getFeatures().getMonitoring());
        assertNull(result.getMonitoring().getRemoteWriteUrl());
    }

    @Test
    public void testConvertWithDisabledMonitoringFeatureWithCdpSaas() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        FeaturesRequest featuresRequest = new FeaturesRequest();
        featuresRequest.addMonitoring(false);
        telemetryRequest.setFeatures(featuresRequest);
        given(entitlementService.isComputeMonitoringEnabled(anyString())).willReturn(true);
        // WHEN
        EnvironmentTelemetry result = underTest.convert(telemetryRequest, getAccountTelemetry(), ACCOUNT_ID);
        // THEN
        assertFalse(result.getFeatures().getMonitoring().getEnabled());
        assertNotNull(result.getMonitoring().getRemoteWriteUrl());
    }

    @Test
    public void testConvertWithWADisabled() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        FeaturesRequest fr = new FeaturesRequest();
        fr.addWorkloadAnalytics(false);
        telemetryRequest.setFeatures(fr);
        // WHEN
        EnvironmentTelemetry result = underTest.convert(telemetryRequest, getAccountTelemetry(), ACCOUNT_ID);
        // THEN
        assertFalse(result.getFeatures().getWorkloadAnalytics().getEnabled());
    }

    @Test
    void convertTestTelemetryRequestTestWhenNullTelemetry() {
        EnvironmentTelemetry result = underTest.convert(null, getAccountTelemetry(), ACCOUNT_ID);

        assertThat(result).isNull();
    }

    @Test
    void convertTestTelemetryRequestTestWhenNullLogging() {
        TelemetryRequest request = new TelemetryRequest();
        request.setLogging(null);

        EnvironmentTelemetry result = underTest.convert(request, getAccountTelemetry(), ACCOUNT_ID);

        assertThat(result).isNotNull();
        assertThat(result.getLogging()).isNull();
    }

    @Test
    void convertTestTelemetryRequestTestWhenLoggingStorageLocation() {
        TelemetryRequest request = new TelemetryRequest();
        LoggingRequest loggingRequest = new LoggingRequest();
        request.setLogging(loggingRequest);
        loggingRequest.setStorageLocation(STORAGE_LOCATION);

        EnvironmentTelemetry result = underTest.convert(request, getAccountTelemetry(), ACCOUNT_ID);

        assertThat(result).isNotNull();
        EnvironmentLogging logging = result.getLogging();
        assertThat(logging).isNotNull();
        verify(storageLocationDecorator).setLoggingStorageLocationFromRequest(logging, STORAGE_LOCATION);
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
        TelemetryResponse result = underTest.convert(telemetry, ACCOUNT_ID);
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
        TelemetryRequest result = underTest.convertToRequest(telemetry, ACCOUNT_ID);
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
        features.addMonitoring(true);
        EnvironmentTelemetry telemetry = new EnvironmentTelemetry();
        telemetry.setLogging(logging);
        telemetry.setFeatures(features);
        // WHEN
        TelemetryRequest result = underTest.convertToRequest(telemetry, ACCOUNT_ID);
        // THEN
        assertNotNull(result.getFeatures());
        assertTrue(result.getFeatures().getMonitoring().getEnabled());
        assertEquals(INSTANCE_PROFILE_VALUE, result.getLogging().getS3().getInstanceProfile());
    }

    @Test
    void testConvertForEdit() {
        EnvironmentLogging logging = new EnvironmentLogging();
        S3CloudStorageParameters s3Params = new S3CloudStorageParameters();
        s3Params.setInstanceProfile(INSTANCE_PROFILE_VALUE);
        logging.setS3(s3Params);
        EnvironmentTelemetry telemetry = new EnvironmentTelemetry();
        telemetry.setLogging(logging);

        TelemetryRequest request = new TelemetryRequest();
        WorkloadAnalyticsRequest workloadAnalytics = new WorkloadAnalyticsRequest();
        workloadAnalytics.setAttributes(Map.of());
        request.setWorkloadAnalytics(workloadAnalytics);

        underTest.convertForEdit(telemetry, request, null, null);

        assertNotNull(telemetry.getLogging());
        assertNotNull(telemetry.getWorkloadAnalytics());
    }

    private AccountTelemetry getAccountTelemetry() {
        AccountTelemetry accountTelemetry = new AccountTelemetry();
        accountTelemetry.setFeatures(new Features());
        return accountTelemetry;
    }

}
