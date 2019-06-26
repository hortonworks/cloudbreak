package com.sequenceiq.cloudbreak.converter.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.Logging;
import com.sequenceiq.cloudbreak.cloud.model.LoggingOutputType;
import com.sequenceiq.cloudbreak.cloud.model.Telemetry;
import com.sequenceiq.cloudbreak.cloud.model.WorkloadAnalytics;
import com.sequenceiq.environment.api.v1.environment.model.response.LoggingResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.TelemetryResponse;

public class TelemetryMergerUtilTest {

    @Test
    public void testMergeGlobalAndStackLevelTelemetry() {
        // GIVEN
        TelemetryResponse envTelemetry = new TelemetryResponse();
        LoggingResponse loggingResponse = new LoggingResponse();
        loggingResponse.setEnabled(true);
        loggingResponse.setOutput(LoggingOutputType.S3);
        envTelemetry.setLogging(loggingResponse);
        Telemetry stackTelemetry = new Telemetry(
                null,
                new WorkloadAnalytics(true, null, null, null, null)
        );
        // WHEN
        Telemetry result = TelemetryMergerUtil.mergeGlobalAndStackLevelTelemetry(envTelemetry, stackTelemetry);
        // THEN
        assertTrue(result.getLogging().isEnabled());
        assertTrue(result.getWorkloadAnalytics().isEnabled());
    }

    @Test
    public void testMergeGlobalAndStackLevelTelemetryOverrideLogging() {
        // GIVEN
        TelemetryResponse envTelemetry = new TelemetryResponse();
        LoggingResponse loggingResponse = new LoggingResponse();
        loggingResponse.setEnabled(true);
        loggingResponse.setOutput(LoggingOutputType.S3);
        envTelemetry.setLogging(loggingResponse);
        Telemetry stackTelemetry = new Telemetry(
                new Logging(true, LoggingOutputType.WASB, null),
                null
        );
        // WHEN
        Telemetry result = TelemetryMergerUtil.mergeGlobalAndStackLevelTelemetry(envTelemetry, stackTelemetry);
        // THEN
        assertEquals(LoggingOutputType.WASB, result.getLogging().getOutputType());
    }

    @Test
    public void testMergeGlobalAndStackLevelTelemetryWithNulls() {
        // GIVEN
        // WHEN
        Telemetry result = TelemetryMergerUtil.mergeGlobalAndStackLevelTelemetry(null, null);
        // THEN
        assertNull(result.getLogging());
        assertNull(result.getWorkloadAnalytics());
    }
}
