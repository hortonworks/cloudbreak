package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.WorkloadAnalytics;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.request.WorkloadAnalyticsRequest;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.telemetry.response.WorkloadAnalyticsResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Component
public class TelemetryConverter {

    private static final String DATABUS_HEADER_SDX_ID = "databus.header.sdx.id";

    private static final String DATABUS_HEADER_SDX_NAME = "databus.header.sdx.name";

    private final boolean telemetryPublisherEnabled;

    private final String databusEndpoint;

    private final boolean meteringEnabled;

    private final boolean reportDeploymentLogs;

    public TelemetryConverter(
            @Value("${cb.cm.telemetrypublisher.enabled:false}") boolean telemetryPublisherEnabled,
            @Value("${metering.enabled:false}") boolean meteringEnabled,
            @Value("${cluster.deployment.logs.report:false}") boolean reportDeploymentLogs,
            @Value("${altus.databus.endpoint:}") String databusEndpoint) {
        this.telemetryPublisherEnabled = telemetryPublisherEnabled;
        this.databusEndpoint = databusEndpoint;
        this.meteringEnabled = meteringEnabled;
        this.reportDeploymentLogs = reportDeploymentLogs;
    }

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
            response.setReportDeploymentLogs(telemetry.isReportDeploymentLogs());
        }
        return response;
    }

    public Telemetry convert(TelemetryRequest request) {
        Telemetry telemetry = new Telemetry();
        if (request != null) {
            Logging logging = null;
            WorkloadAnalytics workloadAnalytics = null;
            if (request.getLogging() != null) {
                LoggingRequest loggingRequest = request.getLogging();
                logging = new Logging();
                logging.setStorageLocation(loggingRequest.getStorageLocation());
                logging.setS3(loggingRequest.getS3());
                logging.setWasb(loggingRequest.getWasb());
            }
            if (request.getWorkloadAnalytics() != null) {
                WorkloadAnalyticsRequest workloadAnalyticsRequest = request.getWorkloadAnalytics();
                workloadAnalytics = new WorkloadAnalytics();
                workloadAnalytics.setAttributes(workloadAnalyticsRequest.getAttributes());
                workloadAnalytics.setDatabusEndpoint(workloadAnalyticsRequest.getDatabusEndpoint());
            }
            telemetry = new Telemetry();
            telemetry.setLogging(logging);
            telemetry.setWorkloadAnalytics(workloadAnalytics);
            if (reportDeploymentLogs) {
                telemetry.setReportDeploymentLogs(request.getReportDeploymentLogs());
            }
        }
        if (meteringEnabled) {
            telemetry.setMeteringEnabled(true);
        }
        if (StringUtils.isNotEmpty(databusEndpoint)) {
            telemetry.setDatabusEndpoint(databusEndpoint);
        }
        return telemetry;
    }

    public TelemetryRequest convert(TelemetryResponse response,
            SdxClusterResponse sdxClusterResponse, boolean enableWorkloadAnalytics) {
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        if (response != null) {
            LoggingRequest loggingRequest = null;
            if (response.getLogging() != null) {
                LoggingResponse loggingResponse = response.getLogging();
                loggingRequest = new LoggingRequest();
                loggingRequest.setStorageLocation(loggingResponse.getStorageLocation());
                loggingRequest.setS3(loggingResponse.getS3());
                loggingRequest.setWasb(loggingResponse.getWasb());
            }
            telemetryRequest.setLogging(loggingRequest);
            telemetryRequest.setReportDeploymentLogs(response.getReportDeploymentLogs());
        }
        telemetryRequest.setWorkloadAnalytics(
                createWorkloadAnalyticsRequest(response, sdxClusterResponse, enableWorkloadAnalytics));
        return telemetryRequest;
    }

    private WorkloadAnalyticsRequest createWorkloadAnalyticsRequest(TelemetryResponse response,
            SdxClusterResponse sdxClusterResponse, boolean enableWorkloadAnalytics) {
        WorkloadAnalyticsRequest workloadAnalyticsRequest = null;
        if (telemetryPublisherEnabled) {
            if (enableWorkloadAnalytics) {
                workloadAnalyticsRequest = new WorkloadAnalyticsRequest();
                if (StringUtils.isNotEmpty(databusEndpoint)) {
                    workloadAnalyticsRequest.setDatabusEndpoint(databusEndpoint);
                    workloadAnalyticsRequest.setAttributes(enrichWithSdxData(new HashMap<>(), sdxClusterResponse));
                }
            } else if (response != null && response.getWorkloadAnalytics() != null) {
                WorkloadAnalyticsResponse waResponse = response.getWorkloadAnalytics();
                workloadAnalyticsRequest = new WorkloadAnalyticsRequest();
                workloadAnalyticsRequest.setAttributes(
                        enrichWithSdxData(waResponse.getAttributes(), sdxClusterResponse));
                workloadAnalyticsRequest.setDatabusEndpoint(waResponse.getDatabusEndpoint());
            }
        }
        return workloadAnalyticsRequest;
    }

    private Map<String, Object> enrichWithSdxData(Map<String, Object> attributes,
            SdxClusterResponse sdxClusterResponse) {
        Map<String, Object> newAttributes = new HashMap<>(attributes);
        if (sdxClusterResponse != null) {
            if (StringUtils.isNotEmpty(sdxClusterResponse.getCrn())) {
                newAttributes.put(DATABUS_HEADER_SDX_ID,
                        Crn.fromString(sdxClusterResponse.getCrn()).getResource());
            }
            if (StringUtils.isNotEmpty(sdxClusterResponse.getName())) {
                newAttributes.put(DATABUS_HEADER_SDX_NAME, sdxClusterResponse.getName());
            }
        }
        return newAttributes;
    }

    public TelemetryRequest convertToRequest(Telemetry telemetry) {
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        if (telemetry != null) {
            Logging logging = telemetry.getLogging();
            if (logging != null) {
                LoggingRequest loggingRequest = new LoggingRequest();
                loggingRequest.setS3(logging.getS3());
                loggingRequest.setWasb(logging.getWasb());
                loggingRequest.setStorageLocation(logging.getStorageLocation());
                loggingRequest.setAttributes(logging.getAttributes());
                telemetryRequest.setLogging(loggingRequest);
            }
            WorkloadAnalytics workloadAnalytics = telemetry.getWorkloadAnalytics();
            if (workloadAnalytics != null) {
                WorkloadAnalyticsRequest waRequest = new WorkloadAnalyticsRequest();
                waRequest.setDatabusEndpoint(workloadAnalytics.getDatabusEndpoint());
                waRequest.setAttributes(workloadAnalytics.getAttributes());
                telemetryRequest.setWorkloadAnalytics(waRequest);
            }
            telemetryRequest.setReportDeploymentLogs(telemetry.isReportDeploymentLogs());
        }
        return telemetryRequest;
    }
}
