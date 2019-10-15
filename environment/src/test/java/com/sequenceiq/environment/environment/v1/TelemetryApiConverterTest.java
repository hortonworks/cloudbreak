package com.sequenceiq.environment.environment.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.request.WorkloadAnalyticsRequest;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.dto.telemetry.S3CloudStorageParameters;

public class TelemetryApiConverterTest {

    private static final String INSTANCE_PROFILE_VALUE = "myInstanceProfile";

    private TelemetryApiConverter underTest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new TelemetryApiConverter(false, "http://mydatabus.endpoint.com");
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
        // WHEN
        EnvironmentTelemetry result = underTest.convert(telemetryRequest);
        // THEN
        assertEquals(INSTANCE_PROFILE_VALUE, result.getLogging().getS3().getInstanceProfile());
        assertNotNull(result.getWorkloadAnalytics());
        assertEquals(result.getWorkloadAnalytics().getDatabusEndpoint(), "http://mydatabus.endpoint.com");
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
    public void testConvertWithDatabusEndpoint() {
        // GIVEN
        TelemetryRequest tr = new TelemetryRequest();
        WorkloadAnalyticsRequest wr = new WorkloadAnalyticsRequest();
        wr.setDatabusEndpoint("customEndpoint");
        tr.setWorkloadAnalytics(wr);
        // WHEN
        EnvironmentTelemetry result = underTest.convert(tr);
        // THEN
        assertNotNull(result.getWorkloadAnalytics());
        assertEquals(result.getWorkloadAnalytics().getDatabusEndpoint(), "customEndpoint");
    }

}
