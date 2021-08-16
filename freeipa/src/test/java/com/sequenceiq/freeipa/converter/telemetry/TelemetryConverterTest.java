package com.sequenceiq.freeipa.converter.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfiguration;
import com.sequenceiq.cloudbreak.telemetry.logcollection.ClusterLogsCollectionConfiguration;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;

public class TelemetryConverterTest {

    private static final String INSTANCE_PROFILE_VALUE = "myInstanceProfile";

    private static final String DATABUS_ENDPOINT = "myCustomEndpoint";

    private static final String DATABUS_S3_BUCKET = "myCustomS3Bucket";

    private static final String EMAIL = "blah@blah.blah";

    private TelemetryConverter underTest;

    @BeforeEach
    public void setUp() {
        AltusDatabusConfiguration altusDatabusConfiguration = new AltusDatabusConfiguration(DATABUS_ENDPOINT, DATABUS_S3_BUCKET, false, "", null);
        MeteringConfiguration meteringConfiguration = new MeteringConfiguration(false, null, null);
        ClusterLogsCollectionConfiguration logCollectionConfig = new ClusterLogsCollectionConfiguration(true, null, null);
        MonitoringConfiguration monitoringConfig = new MonitoringConfiguration(true, null, null);
        TelemetryConfiguration telemetryConfiguration =
                new TelemetryConfiguration(altusDatabusConfiguration, meteringConfiguration, logCollectionConfig, monitoringConfig, null);
        underTest = new TelemetryConverter(telemetryConfiguration, true);
    }

    @Test
    public void testConvertFromRequest() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        LoggingRequest logging = new LoggingRequest();
        logging.setS3(new S3CloudStorageV1Parameters());
        FeaturesRequest featuresRequest = new FeaturesRequest();
        featuresRequest.addClusterLogsCollection(false);
        telemetryRequest.setLogging(logging);
        telemetryRequest.setFeatures(featuresRequest);
        // WHEN
        Telemetry result = underTest.convert(telemetryRequest);
        // THEN
        assertThat(result.getFeatures().getWorkloadAnalytics(), nullValue());
        assertThat(result.getFeatures().getClusterLogsCollection().isEnabled(), is(false));
        assertThat(result.getFeatures().getCloudStorageLogging().isEnabled(), is(true));
        assertThat(result.getFeatures().getMonitoring().isEnabled(), is(true));
        assertThat(result.getDatabusEndpoint(), is(DATABUS_ENDPOINT));
    }

    @Test
    public void testConvertToResponse() {
        Logging logging = new Logging();
        S3CloudStorageV1Parameters s3Params = new S3CloudStorageV1Parameters();
        s3Params.setInstanceProfile(INSTANCE_PROFILE_VALUE);
        logging.setS3(s3Params);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        // WHEN
        TelemetryResponse result = underTest.convert(telemetry);
        // THEN
        assertThat(result.getLogging().getS3().getInstanceProfile(), is(INSTANCE_PROFILE_VALUE));
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
        featuresRequest.addClusterLogsCollection(false);
        telemetryRequest.setLogging(logging);
        telemetryRequest.setFeatures(featuresRequest);
        // WHEN
        Telemetry result = underTest.convert(telemetryRequest);
        // THEN
        assertThat(result.getFeatures().getWorkloadAnalytics(), nullValue());
        assertThat(result.getFeatures().getClusterLogsCollection().isEnabled(), is(false));
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
