package com.sequenceiq.environment.credential.v1.converter;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Logging;
import com.sequenceiq.cloudbreak.cloud.model.Telemetry;
import com.sequenceiq.cloudbreak.cloud.model.WorkloadAnalytics;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.api.v1.environment.model.request.LoggingRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.TelemetryRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.WorkloadAnalyticsRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.LoggingResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.TelemetryResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.WorkloadAnalyticsResponse;

@Component
public class TelemetryApiConverter {

    public Telemetry convert(TelemetryRequest request) {
        Telemetry telemetry = null;
        if (request != null) {
            Logging logging = null;
            WorkloadAnalytics wa = null;
            if (request.getLogging() != null) {
                LoggingRequest loggingRequest = request.getLogging();
                logging = new Logging(
                        loggingRequest.isEnabled(),
                        loggingRequest.getOutput(),
                        loggingRequest.getAttributes());
            }
            if (request.getWorkloadAnalytics() != null) {
                WorkloadAnalyticsRequest waRequest = request.getWorkloadAnalytics();
                wa = new WorkloadAnalytics(waRequest.isEnabled(), waRequest.getDatabusEndpoint(),
                        null, null, waRequest.getAttributes());
            }
            telemetry = new Telemetry(logging, wa);
        }
        return telemetry;
    }

    public TelemetryResponse convertFromJson(Json json) {
        if (json != null && StringUtils.isNotEmpty(json.getValue())) {
            Telemetry telemetry = null;
            try {
                telemetry = json.get(Telemetry.class);
            } catch (IOException e) {
                throw new IllegalArgumentException("Telemetry JSON is not valid", e);
            }
            return convert(telemetry);
        }
        return null;
    }

    public TelemetryResponse convert(Telemetry telemetry) {
        TelemetryResponse response = null;
        if (telemetry != null) {
            LoggingResponse loggingResponse = null;
            WorkloadAnalyticsResponse waResponse = null;
            if (telemetry.getLogging() != null) {
                Logging logging = telemetry.getLogging();
                loggingResponse = new LoggingResponse();
                loggingResponse.setAttributes(logging.getAttributes());
                loggingResponse.setOutput(logging.getOutputType());
                loggingResponse.setEnabled(logging.isEnabled());
            }
            if (telemetry.getWorkloadAnalytics() != null) {
                WorkloadAnalytics wa = telemetry.getWorkloadAnalytics();
                waResponse = new WorkloadAnalyticsResponse();
                waResponse.setEnabled(wa.isEnabled());
                waResponse.setAttributes(wa.getAttributes());
                waResponse.setDatabusEndpoint(wa.getDatabusEndpoint());
            }
            response = new TelemetryResponse();
            response.setLogging(loggingResponse);
            response.setWorkloadAnalytics(waResponse);
        }
        return response;
    }
}
