package com.sequenceiq.freeipa.converter.telemetry;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfiguration;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.FeatureSetting;

public class TelemetryConverterTest {

    private static final String INSTANCE_PROFILE_VALUE = "myInstanceProfile";

    private static final String DATABUS_ENDPOINT = "myCustomEndpoint";

    private TelemetryConverter underTest;

    @BeforeEach
    public void setUp() {
        AltusDatabusConfiguration altusDatabusConfiguration = new AltusDatabusConfiguration(DATABUS_ENDPOINT, false, false, "", null);
        TelemetryConfiguration telemetryConfiguration = new TelemetryConfiguration(altusDatabusConfiguration, true, true, false);
        underTest = new TelemetryConverter(telemetryConfiguration, true);
    }

    @Test
    public void testConvertFromRequest() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        LoggingRequest logging = new LoggingRequest();
        logging.setS3(new S3CloudStorageV1Parameters());
        FeaturesRequest featuresRequest = new FeaturesRequest();
        FeatureSetting reportDeploymentLogs = new FeatureSetting();
        reportDeploymentLogs.setEnabled(false);
        featuresRequest.setReportDeploymentLogs(reportDeploymentLogs);
        telemetryRequest.setLogging(logging);
        telemetryRequest.setFeatures(featuresRequest);
        // WHEN
        Telemetry result = underTest.convert(telemetryRequest);
        // THEN
        assertThat(result.getFeatures().getWorkloadAnalytics(), nullValue());
        assertThat(result.getFeatures().getReportDeploymentLogs().isEnabled(), is(false));
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

}
