package com.sequenceiq.cloudbreak.converter.v4.stacks;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.WorkloadAnalytics;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.telemetry.response.WorkloadAnalyticsResponse;

@Component
public class TelemetryConverter {

    public TelemetryResponse convert(Telemetry telemetry) {
        TelemetryResponse response = null;
        if (telemetry != null) {
            LoggingResponse loggingResponse = null;
            WorkloadAnalyticsResponse waResponse = null;
            if (telemetry.getLogging() != null) {
                Logging logging = telemetry.getLogging();
                loggingResponse = new LoggingResponse();
                loggingResponse.setStorageLocation(logging.getStorageLocation());
                loggingResponse.setS3(logging.getS3());
                loggingResponse.setWasb(logging.getWasb());
            }
            if (telemetry.getWorkloadAnalytics() != null) {
                WorkloadAnalytics workloadAnalytics = telemetry.getWorkloadAnalytics();
                waResponse = new WorkloadAnalyticsResponse();
                waResponse.setAttributes(workloadAnalytics.getAttributes());
                waResponse.setDatabusEndpoint(workloadAnalytics.getDatabusEndpoint());
            }
            response = new TelemetryResponse();
            response.setLogging(loggingResponse);
            response.setWorkloadAnalytics(waResponse);
        }
        return response;
    }

    public Telemetry convert(TelemetryResponse response) {
        Telemetry telemetry = null;
        if (response != null) {
            Logging logging = null;
            WorkloadAnalytics workloadAnalytics = null;
            if (response.getLogging() != null) {
                LoggingResponse loggingResponse = response.getLogging();
                logging = new Logging();
                logging.setStorageLocation(loggingResponse.getStorageLocation());
                logging.setS3(loggingResponse.getS3());
                logging.setWasb(loggingResponse.getWasb());
            }
            if (response.getWorkloadAnalytics() != null) {
                WorkloadAnalyticsResponse waResponse = response.getWorkloadAnalytics();
                workloadAnalytics = new WorkloadAnalytics();
                workloadAnalytics.setAttributes(waResponse.getAttributes());
                workloadAnalytics.setDatabusEndpoint(waResponse.getDatabusEndpoint());
            }
            telemetry = new Telemetry(logging, workloadAnalytics);
        }
        return telemetry;
    }
}
