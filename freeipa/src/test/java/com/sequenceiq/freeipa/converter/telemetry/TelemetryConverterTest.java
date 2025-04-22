package com.sequenceiq.freeipa.converter.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringUrlResolver;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Monitoring;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.MonitoringRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;

@ExtendWith(MockitoExtension.class)
public class TelemetryConverterTest {

    private static final String INSTANCE_PROFILE_VALUE = "myInstanceProfile";

    private static final String DATABUS_ENDPOINT = "myCustomEndpoint";

    private static final String DATABUS_S3_BUCKET = "myCustomS3Bucket";

    private static final String MONITORING_REMOTE_WRITE_URL = "http://myendpoint/api/v1/write";

    private static final String EMAIL = "blah@blah.blah";

    private static final String ACCOUNT_ID = "account1";

    @Mock
    private MonitoringUrlResolver monitoringUrlResolver;

    @Mock
    private EntitlementService entitlementService;

    private TelemetryConverter underTest;

    @BeforeEach
    public void setUp() {
        AltusDatabusConfiguration altusDatabusConfiguration = new AltusDatabusConfiguration(DATABUS_ENDPOINT, DATABUS_S3_BUCKET, false, "", null);
        MonitoringConfiguration monitoringConfig = new MonitoringConfiguration();
        TelemetryConfiguration telemetryConfiguration =
                new TelemetryConfiguration(altusDatabusConfiguration, monitoringConfig, null);
        MockitoAnnotations.openMocks(this);
        underTest = new TelemetryConverter(telemetryConfiguration, true, monitoringUrlResolver, entitlementService);
    }

    @Test
    public void testConvertFromRequest() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        LoggingRequest logging = new LoggingRequest();
        logging.setS3(new S3CloudStorageV1Parameters());
        FeaturesRequest featuresRequest = new FeaturesRequest();
        telemetryRequest.setLogging(logging);
        MonitoringRequest monitoringRequest = new MonitoringRequest();
        monitoringRequest.setRemoteWriteUrl(MONITORING_REMOTE_WRITE_URL);
        telemetryRequest.setMonitoring(monitoringRequest);
        telemetryRequest.setFeatures(featuresRequest);
        when(entitlementService.isComputeMonitoringEnabled(anyString())).thenReturn(true);
        when(monitoringUrlResolver.resolve(anyString(), anyString())).thenReturn(MONITORING_REMOTE_WRITE_URL);
        // WHEN
        Telemetry result = underTest.convert(ACCOUNT_ID, telemetryRequest);
        // THEN
        assertThat(result.getFeatures().getWorkloadAnalytics(), nullValue());
        assertThat(result.getFeatures().getCloudStorageLogging().getEnabled(), is(true));
        assertThat(result.getFeatures().getMonitoring().getEnabled(), is(true));
        assertThat(result.getDatabusEndpoint(), is(DATABUS_ENDPOINT));
        assertThat(result.getMonitoring().getRemoteWriteUrl(), is(MONITORING_REMOTE_WRITE_URL));
    }

    @Test
    public void testConvertToResponse() {
        Logging logging = new Logging();
        S3CloudStorageV1Parameters s3Params = new S3CloudStorageV1Parameters();
        s3Params.setInstanceProfile(INSTANCE_PROFILE_VALUE);
        logging.setS3(s3Params);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        Monitoring monitoring = new Monitoring();
        monitoring.setRemoteWriteUrl(MONITORING_REMOTE_WRITE_URL);
        telemetry.setMonitoring(monitoring);
        // WHEN
        TelemetryResponse result = underTest.convert(telemetry);
        // THEN
        assertThat(result.getLogging().getS3().getInstanceProfile(), is(INSTANCE_PROFILE_VALUE));
        assertThat(result.getMonitoring().getRemoteWriteUrl(), is(MONITORING_REMOTE_WRITE_URL));
    }

    @Test
    public void testConvertFromRequestForGCS() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        LoggingRequest logging = new LoggingRequest();
        GcsCloudStorageV1Parameters gcsCloudStorageV1Parameters = new GcsCloudStorageV1Parameters();
        gcsCloudStorageV1Parameters.setServiceAccountEmail(EMAIL);
        logging.setGcs(gcsCloudStorageV1Parameters);
        FeaturesRequest featuresRequest = new FeaturesRequest();
        telemetryRequest.setLogging(logging);
        telemetryRequest.setFeatures(featuresRequest);
        // WHEN
        Telemetry result = underTest.convert(ACCOUNT_ID, telemetryRequest);
        // THEN
        assertThat(result.getFeatures().getWorkloadAnalytics(), nullValue());
        assertThat(result.getDatabusEndpoint(), is(DATABUS_ENDPOINT));
        assertThat(result.getLogging().getGcs(), notNullValue());
        assertThat(result.getLogging().getGcs().getServiceAccountEmail(), is(EMAIL));
    }

    @Test
    public void testConvertToResponseForGcs() {
        Logging logging = new Logging();
        GcsCloudStorageV1Parameters gcsCloudStorageV1Parameters = new GcsCloudStorageV1Parameters();
        gcsCloudStorageV1Parameters.setServiceAccountEmail(EMAIL);
        logging.setGcs(gcsCloudStorageV1Parameters);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        TelemetryResponse result = underTest.convert(telemetry);
        // THEN
        assertThat(result.getLogging().getGcs(), notNullValue());
        assertThat(result.getLogging().getGcs().getServiceAccountEmail(), is(EMAIL));
    }

}
