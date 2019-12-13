package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
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

    private final boolean telemetryPublisherDefaultValue;

    private final String databusEndpoint;

    private final boolean meteringEnabled;

    private final boolean reportDeploymentLogs;

    public TelemetryConverter(
            @Value("${cb.cm.telemetrypublisher.enabled:false}") boolean telemetryPublisherEnabled,
            @Value("${cb.cm.telemetrypublisher.default:true}") boolean telemetryPublisherDefaultValue,
            @Value("${metering.enabled:false}") boolean meteringEnabled,
            @Value("${cluster.deployment.logs.report:false}") boolean reportDeploymentLogs,
            @Value("${altus.databus.endpoint:}") String databusEndpoint) {
        this.telemetryPublisherEnabled = telemetryPublisherEnabled;
        this.telemetryPublisherDefaultValue = telemetryPublisherDefaultValue;
        this.databusEndpoint = databusEndpoint;
        this.meteringEnabled = meteringEnabled;
        this.reportDeploymentLogs = reportDeploymentLogs;
    }

    public TelemetryResponse convert(Telemetry telemetry) {
        TelemetryResponse response = null;
        if (telemetry != null) {
            LoggingResponse loggingResponse = createLoggingResponseFromSource(telemetry);
            WorkloadAnalyticsResponse waResponse = createWorkloadAnalyticsResponseFromSource(telemetry);
            response = new TelemetryResponse();
            response.setLogging(loggingResponse);
            response.setWorkloadAnalytics(waResponse);
            response.setFluentAttributes(telemetry.getFluentAttributes());
            createFeaturesResponseFromSource(response, telemetry.getFeatures());
        }
        return response;
    }

    public Telemetry convert(TelemetryRequest request, StackType type) {
        Telemetry telemetry = new Telemetry();
        Features features = new Features();
        LOGGER.debug("Converting telemetry request to telemetry object");
        if (request != null) {
            Logging logging = createLoggingFromRequest(request);
            WorkloadAnalytics workloadAnalytics = createWorkloadAnalyticsFromRequest(request);
            telemetry = new Telemetry();
            telemetry.setLogging(logging);
            telemetry.setWorkloadAnalytics(workloadAnalytics);
            setWorkloadAnalyticsFeature(telemetry, features);
            setReportDeploymentLogs(request, features);
            telemetry.setFluentAttributes(request.getFluentAttributes());
        }
        setMeteringFeature(type, features);
        if (StringUtils.isNotEmpty(databusEndpoint)) {
            LOGGER.debug("Setting databus endpoint: {}", databusEndpoint);
            telemetry.setDatabusEndpoint(databusEndpoint);
        }
        telemetry.setFeatures(features);
        return telemetry;
    }

    public TelemetryRequest convert(TelemetryResponse response,
            SdxClusterResponse sdxClusterResponse) {
        LOGGER.debug("Creating telemetry request based on datalake and environment responses.");
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        FeaturesRequest featuresRequest = new FeaturesRequest();
        if (response != null) {
            LoggingRequest loggingRequest = createLoggingRequestFromResponse(response);
            telemetryRequest.setLogging(loggingRequest);
            FeaturesResponse featuresResponse = response.getFeatures();
            if (featuresResponse != null) {
                LOGGER.debug("Setting report deployment logs response (telemetry) based on environment response.");
                featuresRequest.setReportDeploymentLogs(featuresResponse.getReportDeploymentLogs());
            }
            telemetryRequest.setFluentAttributes(response.getFluentAttributes());
        }
        telemetryRequest.setWorkloadAnalytics(
                createWorkloadAnalyticsRequest(response, sdxClusterResponse));
        Optional<FeatureSetting> waFeature = createWorkloadAnalyticsFeature(telemetryRequest.getWorkloadAnalytics());
        featuresRequest.setWorkloadAnalytics(waFeature.orElse(null));
        telemetryRequest.setFeatures(featuresRequest);
        return telemetryRequest;
    }

    public TelemetryRequest convertToRequest(Telemetry telemetry) {
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        if (telemetry != null) {
            LoggingRequest loggingRequest = createLoggingRequestFromSource(telemetry);
            WorkloadAnalyticsRequest waRequest = createWorkloadAnalyticsRequestFromSource(telemetry);
            FeaturesRequest featuresRequest = createFeaturesRequestFromSource(telemetry);

            telemetryRequest.setWorkloadAnalytics(waRequest);
            telemetryRequest.setLogging(loggingRequest);
            telemetryRequest.setFluentAttributes(telemetry.getFluentAttributes());
            telemetryRequest.setFeatures(featuresRequest);
        }
        return telemetryRequest;
    }

    public Optional<FeatureSetting> createWorkloadAnalyticsFeature(WorkloadAnalyticsRequest waRequest) {
        boolean waFeatureEnabled = false;
        if (waRequest != null) {
            waFeatureEnabled = true;
            LOGGER.debug("Workload analytics feature (enable) because workload analytics request has been set already.");
        } else {
            LOGGER.debug("Workload analytics feature disabled because workload analytics request has not been set.");
        }
        FeatureSetting waFeatureSetting = new FeatureSetting();
        waFeatureSetting.setEnabled(waFeatureEnabled);
        return Optional.of(waFeatureSetting);
    }

    public WorkloadAnalyticsRequest createWorkloadAnalyticsRequest(TelemetryResponse response,
            SdxClusterResponse sdxClusterResponse) {
        WorkloadAnalyticsRequest workloadAnalyticsRequest = null;
        if (telemetryPublisherEnabled) {
            Map<String, Object> waDefaultAttributes = createWAAttributesFromEnvironmentResponse(response);
            workloadAnalyticsRequest = fillWARequestFromEnvironmenResponse(response, sdxClusterResponse, waDefaultAttributes);
        } else {
            LOGGER.debug("Workload analytics feature is disabled (globally).");
        }
        return workloadAnalyticsRequest;
    }

    private WorkloadAnalyticsRequest fillWARequestFromEnvironmenResponse(TelemetryResponse response,
            SdxClusterResponse sdxClusterResponse, Map<String, Object> waDefaultAttributes) {
        WorkloadAnalyticsRequest workloadAnalyticsRequest = null;
        if (response != null && response.getFeatures() != null
                && response.getFeatures().getWorkloadAnalytics() != null) {
            if (response.getFeatures().getWorkloadAnalytics().isEnabled()) {
                LOGGER.debug("Workload analytics feature is enabled. Filling telemetry request with datalake details.");
                workloadAnalyticsRequest = new WorkloadAnalyticsRequest();
                workloadAnalyticsRequest.setAttributes(enrichWithSdxData(waDefaultAttributes, sdxClusterResponse));
            } else {
                LOGGER.debug("Workload analytics feature is disabled.");
            }
        } else {
            if (telemetryPublisherDefaultValue) {
                LOGGER.debug("Filling workload analytics request (default).");
                workloadAnalyticsRequest = new WorkloadAnalyticsRequest();
                workloadAnalyticsRequest.setAttributes(enrichWithSdxData(waDefaultAttributes, sdxClusterResponse));
            } else {
                LOGGER.debug("Workload analytics feature is disabled (default value is false).");
            }
        }
        return workloadAnalyticsRequest;
    }

    private Map<String, Object> createWAAttributesFromEnvironmentResponse(TelemetryResponse response) {
        Map<String, Object> waDefaultAttributes = new HashMap<>();
        if (response != null && response.getWorkloadAnalytics() != null
                && MapUtils.isNotEmpty(response.getWorkloadAnalytics().getAttributes())) {
            LOGGER.debug("Found environment level workload analytics attributes.");
            waDefaultAttributes = new HashMap<>(response.getWorkloadAnalytics().getAttributes());
        }
        return waDefaultAttributes;
    }

    private FeaturesRequest createFeaturesRequestFromSource(Telemetry telemetry) {
        FeaturesRequest featuresRequest = null;
        Features features = telemetry.getFeatures();
        if (features != null) {
            featuresRequest = new FeaturesRequest();
            featuresRequest.setWorkloadAnalytics(features.getWorkloadAnalytics());
            featuresRequest.setReportDeploymentLogs(features.getReportDeploymentLogs());
        }
        return featuresRequest;
    }

    private WorkloadAnalyticsRequest createWorkloadAnalyticsRequestFromSource(Telemetry telemetry) {
        WorkloadAnalyticsRequest waRequest = null;
        WorkloadAnalytics workloadAnalytics = telemetry.getWorkloadAnalytics();
        if (workloadAnalytics != null) {
            waRequest = new WorkloadAnalyticsRequest();
            waRequest.setAttributes(workloadAnalytics.getAttributes());
        }
        return waRequest;
    }

    private LoggingRequest createLoggingRequestFromSource(Telemetry telemetry) {
        LoggingRequest loggingRequest = null;
        Logging logging = telemetry.getLogging();
        if (logging != null) {
            loggingRequest = new LoggingRequest();
            loggingRequest.setS3(logging.getS3());
            loggingRequest.setAdlsGen2(logging.getAdlsGen2());
            loggingRequest.setStorageLocation(logging.getStorageLocation());
        }
        return loggingRequest;
    }

    private void setMeteringFeature(StackType type, Features features) {
        if (meteringEnabled && StackType.WORKLOAD.equals(type)) {
            LOGGER.debug("Setting metering for workload cluster (as metering is enabled)");
            FeatureSetting metering = new FeatureSetting();
            metering.setEnabled(true);
            features.setMetering(metering);
        } else {
            LOGGER.debug("Metering feature is disabled - global setting; {}, stack type: {}", meteringEnabled, type);
        }
    }

    private void setWorkloadAnalyticsFeature(Telemetry telemetry, Features features) {
        if (telemetry.getWorkloadAnalytics() != null) {
            LOGGER.debug("Setting workload analytics feature settings as workload analytics request exists.");
            FeatureSetting waFeature = new FeatureSetting();
            waFeature.setEnabled(true);
            features.setWorkloadAnalytics(waFeature);
        } else {
            LOGGER.debug("Workload analytics feature is not enabled.");
        }
    }

    private Logging createLoggingFromRequest(TelemetryRequest request) {
        Logging logging = null;
        if (request.getLogging() != null) {
            LOGGER.debug("Create loggging telemetry settings from logging request.");
            LoggingRequest loggingRequest = request.getLogging();
            logging = new Logging();
            logging.setStorageLocation(loggingRequest.getStorageLocation());
            logging.setS3(loggingRequest.getS3());
            logging.setAdlsGen2(loggingRequest.getAdlsGen2());
        }
        return logging;
    }

    private WorkloadAnalytics createWorkloadAnalyticsFromRequest(TelemetryRequest request) {
        WorkloadAnalytics workloadAnalytics = null;
        if (request.getWorkloadAnalytics() != null) {
            LOGGER.debug("Create workload analytics telemetry settings from workload analytics request.");
            WorkloadAnalyticsRequest workloadAnalyticsRequest = request.getWorkloadAnalytics();
            workloadAnalytics = new WorkloadAnalytics();
            workloadAnalytics.setAttributes(workloadAnalyticsRequest.getAttributes());
            workloadAnalytics.setDatabusEndpoint(databusEndpoint);
        }
        return workloadAnalytics;
    }

    private LoggingResponse createLoggingResponseFromSource(Telemetry telemetry) {
        LoggingResponse loggingResponse = null;
        if (telemetry.getLogging() != null) {
            LOGGER.debug("Setting logging telemetry settings (response).");
            Logging logging = telemetry.getLogging();
            loggingResponse = new LoggingResponse();
            loggingResponse.setStorageLocation(logging.getStorageLocation());
            loggingResponse.setS3(logging.getS3());
            loggingResponse.setAdlsGen2(logging.getAdlsGen2());
        }
        return loggingResponse;
    }

    private WorkloadAnalyticsResponse createWorkloadAnalyticsResponseFromSource(Telemetry telemetry) {
        WorkloadAnalyticsResponse waResponse = null;
        if (telemetry.getWorkloadAnalytics() != null) {
            LOGGER.debug("Setting workload analytics telemetry settings (response).");
            WorkloadAnalytics workloadAnalytics = telemetry.getWorkloadAnalytics();
            waResponse = new WorkloadAnalyticsResponse();
            waResponse.setAttributes(workloadAnalytics.getAttributes());
        }
        return waResponse;
    }

    private void createFeaturesResponseFromSource(TelemetryResponse response, Features features) {
        if (features != null) {
            LOGGER.debug("Setting feature telemetry response.");
            FeaturesResponse featuresResponse = new FeaturesResponse();
            featuresResponse.setWorkloadAnalytics(features.getWorkloadAnalytics());
            featuresResponse.setReportDeploymentLogs(features.getReportDeploymentLogs());
            featuresResponse.setMetering(features.getMetering());
            response.setFeatures(featuresResponse);
        }
    }

    private LoggingRequest createLoggingRequestFromResponse(TelemetryResponse response) {
        LoggingRequest loggingRequest = null;
        if (response.getLogging() != null) {
            LOGGER.debug("Setting logging response (telemetry) based on environment response");
            LoggingResponse loggingResponse = response.getLogging();
            loggingRequest = new LoggingRequest();
            loggingRequest.setStorageLocation(loggingResponse.getStorageLocation());
            loggingRequest.setS3(loggingResponse.getS3());
            loggingRequest.setAdlsGen2(loggingResponse.getAdlsGen2());
        }
        return loggingRequest;
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
