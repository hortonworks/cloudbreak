package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.WorkloadAnalytics;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.request.WorkloadAnalyticsRequest;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.telemetry.response.WorkloadAnalyticsResponse;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Component
public class TelemetryConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryConverter.class);

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
                LOGGER.debug("Setting logging telemetry settings (response).");
                Logging logging = telemetry.getLogging();
                loggingResponse = new LoggingResponse();
                loggingResponse.setStorageLocation(logging.getStorageLocation());
                loggingResponse.setS3(logging.getS3());
                loggingResponse.setWasb(logging.getWasb());
            }
            if (telemetry.getWorkloadAnalytics() != null) {
                LOGGER.debug("Setting workload analytics telemetry settings (response).");
                WorkloadAnalytics workloadAnalytics = telemetry.getWorkloadAnalytics();
                waResponse = new WorkloadAnalyticsResponse();
                waResponse.setAttributes(workloadAnalytics.getAttributes());
            }
            response = new TelemetryResponse();
            response.setLogging(loggingResponse);
            response.setWorkloadAnalytics(waResponse);
            response.setFluentAttributes(telemetry.getFluentAttributes());
            Features features = telemetry.getFeatures();
            if (features != null) {
                LOGGER.debug("Setting feature telemetry response.");
                FeaturesResponse featuresResponse = new FeaturesResponse();
                featuresResponse.setWorkloadAnalytics(features.getWorkloadAnalytics());
                featuresResponse.setReportDeploymentLogs(features.getReportDeploymentLogs());
                featuresResponse.setMetering(features.getMetering());
                response.setFeatures(featuresResponse);
            }
            response.setDatabusEndpoint(databusEndpoint);
        }
        return response;
    }

    public Telemetry convert(TelemetryRequest request, StackType type) {
        Telemetry telemetry = new Telemetry();
        Features features = new Features();
        if (request != null) {
            Logging logging = null;
            WorkloadAnalytics workloadAnalytics = null;
            if (request.getLogging() != null) {
                LOGGER.debug("Create loggging telemetry settings from logging request.");
                LoggingRequest loggingRequest = request.getLogging();
                logging = new Logging();
                logging.setStorageLocation(loggingRequest.getStorageLocation());
                logging.setS3(loggingRequest.getS3());
                logging.setWasb(loggingRequest.getWasb());
            }
            if (request.getWorkloadAnalytics() != null) {
                LOGGER.debug("Create workload analytics telemetry settings from workload analytics request.");
                WorkloadAnalyticsRequest workloadAnalyticsRequest = request.getWorkloadAnalytics();
                workloadAnalytics = new WorkloadAnalytics();
                workloadAnalytics.setAttributes(workloadAnalyticsRequest.getAttributes());
                workloadAnalytics.setDatabusEndpoint(databusEndpoint);
            }
            telemetry = new Telemetry();
            telemetry.setLogging(logging);
            telemetry.setWorkloadAnalytics(workloadAnalytics);
            if (telemetry.getWorkloadAnalytics() != null) {
                LOGGER.debug("Setting workload analytics feature settings as workload analytics request exists.");
                FeatureSetting waFeature = new FeatureSetting();
                waFeature.setEnabled(true);
                features.setWorkloadAnalytics(waFeature);
            }
            setReportDeploymentLogs(request, features);
            telemetry.setFluentAttributes(request.getFluentAttributes());
        }
        if (meteringEnabled && StackType.WORKLOAD.equals(type)) {
            LOGGER.debug("Setting metering for workload cluster (as metering is enabled)");
            FeatureSetting metering = new FeatureSetting();
            metering.setEnabled(true);
            features.setMetering(metering);
        }
        if (StringUtils.isNotEmpty(databusEndpoint)) {
            LOGGER.debug("Setting databus endpoint: {}", databusEndpoint);
            telemetry.setDatabusEndpoint(databusEndpoint);
        }
        telemetry.setFeatures(features);
        return telemetry;
    }

    public TelemetryRequest convert(TelemetryResponse response,
            SdxClusterResponse sdxClusterResponse, FeatureSetting workloadAnalytics) {
        LOGGER.debug("Creating telemetry request based on datalake and environment responses.");
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        FeaturesRequest featuresRequest = new FeaturesRequest();
        if (response != null) {
            LoggingRequest loggingRequest = null;
            if (response.getLogging() != null) {
                LOGGER.debug("Setting logging response (telemetry) based on environment response");
                LoggingResponse loggingResponse = response.getLogging();
                loggingRequest = new LoggingRequest();
                loggingRequest.setStorageLocation(loggingResponse.getStorageLocation());
                loggingRequest.setS3(loggingResponse.getS3());
                loggingRequest.setWasb(loggingResponse.getWasb());
            }
            telemetryRequest.setLogging(loggingRequest);
            FeaturesResponse featuresResponse = response.getFeatures();
            if (featuresResponse != null) {
                LOGGER.debug("Setting report deployment logs response (telemetry) based on environment response.");
                featuresRequest.setReportDeploymentLogs(featuresResponse.getReportDeploymentLogs());
            }
            telemetryRequest.setFluentAttributes(response.getFluentAttributes());
        }
        telemetryRequest.setWorkloadAnalytics(
                createWorkloadAnalyticsRequest(response, sdxClusterResponse, workloadAnalytics));
        Optional<FeatureSetting> waFeature = createWorkloadAnalyticsFeature(telemetryRequest.getWorkloadAnalytics());
        featuresRequest.setWorkloadAnalytics(waFeature.orElse(workloadAnalytics));
        telemetryRequest.setFeatures(featuresRequest);
        return telemetryRequest;
    }

    public Optional<FeatureSetting> createWorkloadAnalyticsFeature(WorkloadAnalyticsRequest waRequest) {
        Optional<FeatureSetting> waFeature = Optional.empty();
        if (waRequest != null) {
            LOGGER.debug("Filling workload analytics feature (enable) because workload analytics request has been set already.");
            FeatureSetting waFeatureSetting = new FeatureSetting();
            waFeatureSetting.setEnabled(true);
            waFeature = Optional.of(waFeatureSetting);
        }
        return waFeature;
    }

    public WorkloadAnalyticsRequest createWorkloadAnalyticsRequest(TelemetryResponse response,
            SdxClusterResponse sdxClusterResponse, FeatureSetting workloadAnalytics) {
        WorkloadAnalyticsRequest workloadAnalyticsRequest = null;
        if (telemetryPublisherEnabled) {
            if (workloadAnalytics != null && workloadAnalytics.isEnabled() != null) {
                if (workloadAnalytics.isEnabled()) {
                    LOGGER.debug("Workload analytics feature is enabled. Filling telemetry request with datalake details.");
                    workloadAnalyticsRequest = new WorkloadAnalyticsRequest();
                    workloadAnalyticsRequest.setAttributes(enrichWithSdxData(new HashMap<>(), sdxClusterResponse));
                } else {
                    LOGGER.debug("Workload analytics feature is disabled.");
                }
            } else if (response != null && response.getWorkloadAnalytics() != null) {
                LOGGER.debug("Workload analytics is set in the telemetry response, fill telemetry request with datalake details..");
                WorkloadAnalyticsResponse waResponse = response.getWorkloadAnalytics();
                workloadAnalyticsRequest = new WorkloadAnalyticsRequest();
                workloadAnalyticsRequest.setAttributes(
                        enrichWithSdxData(waResponse.getAttributes(), sdxClusterResponse));
            } else {
                LOGGER.debug("Filling workload analytics request (default).");
                workloadAnalyticsRequest = new WorkloadAnalyticsRequest();
                workloadAnalyticsRequest.setAttributes(enrichWithSdxData(new HashMap<>(), sdxClusterResponse));
            }
        } else {
            LOGGER.debug("Workload analytics feature is disabled (globally).");
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
                telemetryRequest.setLogging(loggingRequest);
            }
            WorkloadAnalytics workloadAnalytics = telemetry.getWorkloadAnalytics();
            if (workloadAnalytics != null) {
                WorkloadAnalyticsRequest waRequest = new WorkloadAnalyticsRequest();
                waRequest.setAttributes(workloadAnalytics.getAttributes());
                telemetryRequest.setWorkloadAnalytics(waRequest);
            }
            telemetryRequest.setFluentAttributes(telemetry.getFluentAttributes());
            Features features = telemetry.getFeatures();
            if (features != null) {
                FeaturesRequest featuresRequest = new FeaturesRequest();
                featuresRequest.setWorkloadAnalytics(features.getWorkloadAnalytics());
                featuresRequest.setReportDeploymentLogs(features.getReportDeploymentLogs());
                telemetryRequest.setFeatures(featuresRequest);
            }
        }
        return telemetryRequest;
    }

    private void setReportDeploymentLogs(TelemetryRequest request, Features features) {
        if (reportDeploymentLogs) {
            if (request.getFeatures() != null && request.getFeatures().getReportDeploymentLogs() != null) {
                LOGGER.debug("Fill report deployment logs setting from telemetry feature request");
                features.setReportDeploymentLogs(request.getFeatures().getReportDeploymentLogs());
            } else {
                LOGGER.debug("Auto-fill report deployment logs telemetry settings as it is set, but missing from the request.");
                FeatureSetting reportDeploymentLogsFeature = new FeatureSetting();
                reportDeploymentLogsFeature.setEnabled(true);
                features.setReportDeploymentLogs(reportDeploymentLogsFeature);
            }
        } else {
            LOGGER.debug("Report deployment logs feature is disabled. Set feature as false.");
            FeatureSetting reportDeploymentLogsFeature = new FeatureSetting();
            reportDeploymentLogsFeature.setEnabled(false);
            features.setReportDeploymentLogs(reportDeploymentLogsFeature);
        }
    }
}
