package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringUrlResolver;
import com.sequenceiq.common.api.telemetry.base.FeaturesBase;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Monitoring;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.WorkloadAnalytics;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.MonitoringRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.request.WorkloadAnalyticsRequest;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.MonitoringResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.telemetry.response.WorkloadAnalyticsResponse;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentBaseResponse;

@Component
public class TelemetryConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryConverter.class);

    private static final String DATABUS_HEADER_SDX_ID = "databus.header.sdx.id";

    private static final String DATABUS_HEADER_SDX_NAME = "databus.header.sdx.name";

    private static final String DATABUS_HEADER_ENVIRONMENT_CRN = "databus.header.environment.crn";

    private static final String DATABUS_HEADER_ENVIRONMENT_NAME = "databus.header.environment.name";

    private static final String DATABUS_HEADER_DATALAKE_CRN = "databus.header.datalake.crn";

    private static final String DATABUS_HEADER_DATALAKE_NAME = "databus.header.datalake.name";

    private final EntitlementService entitlementService;

    private final boolean telemetryPublisherEnabled;

    private final boolean telemetryPublisherDefaultValue;

    private final String databusEndpoint;

    private final boolean useSharedAltusCredential;

    private final MonitoringUrlResolver monitoringUrlResolver;

    public TelemetryConverter(
            TelemetryConfiguration configuration,
            EntitlementService entitlementService,
            @Value("${cb.cm.telemetrypublisher.enabled:false}") boolean telemetryPublisherEnabled,
            @Value("${cb.cm.telemetrypublisher.default:true}") boolean telemetryPublisherDefaultValue,
            MonitoringUrlResolver monitoringUrlResolver) {
        this.entitlementService = entitlementService;
        this.telemetryPublisherEnabled = telemetryPublisherEnabled;
        this.telemetryPublisherDefaultValue = telemetryPublisherDefaultValue;
        this.databusEndpoint = configuration.getAltusDatabusConfiguration().getAltusDatabusEndpoint();
        this.useSharedAltusCredential = configuration.getAltusDatabusConfiguration().isUseSharedAltusCredential();
        this.monitoringUrlResolver = monitoringUrlResolver;
    }

    public TelemetryResponse convert(Telemetry telemetry) {
        TelemetryResponse response = null;
        if (telemetry != null) {
            LoggingResponse loggingResponse = createLoggingResponseFromSource(telemetry);
            MonitoringResponse monitoringResponse = createMonitoringResponseFromSource(telemetry);
            WorkloadAnalyticsResponse waResponse = createWorkloadAnalyticsResponseFromSource(telemetry);
            response = new TelemetryResponse();
            response.setLogging(loggingResponse);
            response.setMonitoring(monitoringResponse);
            response.setWorkloadAnalytics(waResponse);
            response.setFluentAttributes(telemetry.getFluentAttributes());
            response.setRules(telemetry.getRules());
            createFeaturesResponseFromSource(response, telemetry.getFeatures());
        }
        return response;
    }

    public Telemetry convert(TelemetryRequest request, StackType type, String accountId) {
        Telemetry telemetry = new Telemetry();
        Features features = new Features();
        LOGGER.debug("Converting telemetry request to telemetry object");
        if (request != null) {
            Logging logging = createLoggingFromRequest(request);
            Monitoring monitoring = createMonitoringFromRequest(accountId, request);
            WorkloadAnalytics workloadAnalytics = createWorkloadAnalyticsFromRequest(request);
            telemetry.setLogging(logging);
            telemetry.setMonitoring(monitoring);
            telemetry.setWorkloadAnalytics(workloadAnalytics);
            setWorkloadAnalyticsFeature(telemetry, features);
            setMonitoring(request, features, accountId);
            setUseSharedAltusCredential(features);
            setCloudStorageLogging(request, features);
            telemetry.setFluentAttributes(request.getFluentAttributes());
        }
        if (entitlementService.isComputeMonitoringEnabled(accountId)) {
            LOGGER.debug("Cluster level monitoring feature is enabled");
            features.addMonitoring(true);
        }

        if (StringUtils.isNotEmpty(databusEndpoint)) {
            LOGGER.debug("Setting databus endpoint: {}", databusEndpoint);
            telemetry.setDatabusEndpoint(databusEndpoint);
        }
        telemetry.setFeatures(features);
        LOGGER.info("Created telemetry object for stack type: {} with workload analytics: {}, features: {} and databus endpoint: {}",
                type, telemetry.getWorkloadAnalytics(), features, telemetry.getDatabusEndpoint());
        return telemetry;
    }

    public TelemetryRequest convert(DetailedEnvironmentResponse environment, SdxBasicView sdxBasicView) {
        LOGGER.debug("Creating telemetry request based on datalake and environment responses.");
        TelemetryResponse telemetryResponse = environment != null ? environment.getTelemetry() : null;
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        FeaturesRequest featuresRequest = new FeaturesRequest();
        if (telemetryResponse != null) {
            LoggingRequest loggingRequest = createLoggingRequestFromResponse(telemetryResponse);
            telemetryRequest.setLogging(loggingRequest);
            MonitoringRequest monitoringRequest = createMonitoringRequestFromResponse(telemetryResponse);
            telemetryRequest.setMonitoring(monitoringRequest);
            FeaturesResponse featuresResponse = telemetryResponse.getFeatures();
            if (featuresResponse != null) {
                LOGGER.debug("Setting cluster monitoring request (telemetry) based on environment response.");
                featuresRequest.setMonitoring(featuresResponse.getMonitoring());
                LOGGER.debug("Setting cloud storage logging request (telemetry) based on environment response.");
                if (featuresResponse.getCloudStorageLogging() != null) {
                    featuresRequest.setCloudStorageLogging(featuresResponse.getCloudStorageLogging());
                } else {
                    featuresRequest.addCloudStorageLogging(true);
                }
            }
            telemetryRequest.setFluentAttributes(telemetryResponse.getFluentAttributes());
        }
        telemetryRequest.setWorkloadAnalytics(createWorkloadAnalyticsRequest(environment, sdxBasicView));
        featuresRequest.setWorkloadAnalytics(createWorkloadAnalyticsFeature(telemetryRequest.getWorkloadAnalytics()));
        telemetryRequest.setFeatures(featuresRequest);
        return telemetryRequest;
    }

    public TelemetryRequest convertToRequest(Telemetry telemetry) {
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        if (telemetry != null) {
            LoggingRequest loggingRequest = createLoggingRequestFromSource(telemetry);
            MonitoringRequest monitoringRequest = createMonitoringRequestFromSource(telemetry);
            WorkloadAnalyticsRequest waRequest = createWorkloadAnalyticsRequestFromSource(telemetry);
            FeaturesRequest featuresRequest = createFeaturesRequestFromSource(telemetry);

            telemetryRequest.setWorkloadAnalytics(waRequest);
            telemetryRequest.setLogging(loggingRequest);
            telemetryRequest.setMonitoring(monitoringRequest);
            telemetryRequest.setFluentAttributes(telemetry.getFluentAttributes());
            telemetryRequest.setFeatures(featuresRequest);
        }
        return telemetryRequest;
    }

    private FeatureSetting createWorkloadAnalyticsFeature(WorkloadAnalyticsRequest waRequest) {
        boolean waFeatureEnabled = false;
        if (waRequest != null) {
            waFeatureEnabled = true;
            LOGGER.debug("Workload analytics feature (enable) because workload analytics request has been set already.");
        } else {
            LOGGER.debug("Workload analytics feature disabled because workload analytics request has not been set.");
        }
        FeatureSetting waFeatureSetting = new FeatureSetting();
        waFeatureSetting.setEnabled(waFeatureEnabled);
        return waFeatureSetting;
    }

    private WorkloadAnalyticsRequest createWorkloadAnalyticsRequest(DetailedEnvironmentResponse environment, SdxBasicView sdxBasicView) {
        WorkloadAnalyticsRequest workloadAnalyticsRequest = null;
        if (telemetryPublisherEnabled) {
            Optional<FeatureSetting> workloadAnalyticsFeature = Optional.ofNullable(environment)
                    .map(EnvironmentBaseResponse::getTelemetry)
                    .map(TelemetryResponse::getFeatures)
                    .map(FeaturesBase::getWorkloadAnalytics);
            if (workloadAnalyticsFeature.isPresent()) {
                if (workloadAnalyticsFeature.get().getEnabled()) {
                    LOGGER.debug("Workload analytics feature is enabled. Filling telemetry request with datalake details.");
                    workloadAnalyticsRequest = new WorkloadAnalyticsRequest();
                    workloadAnalyticsRequest.setAttributes(createWorkloadAnalyticsAttributes(environment, sdxBasicView));
                } else {
                    LOGGER.debug("Workload analytics feature is disabled.");
                }
            } else {
                if (telemetryPublisherDefaultValue) {
                    LOGGER.debug("Filling workload analytics request (default).");
                    workloadAnalyticsRequest = new WorkloadAnalyticsRequest();
                    workloadAnalyticsRequest.setAttributes(createWorkloadAnalyticsAttributes(environment, sdxBasicView));
                } else {
                    LOGGER.debug("Workload analytics feature is disabled (default value is false).");
                }
            }
        } else {
            LOGGER.debug("Workload analytics feature is disabled (globally).");
        }
        return workloadAnalyticsRequest;
    }

    private FeaturesRequest createFeaturesRequestFromSource(Telemetry telemetry) {
        FeaturesRequest featuresRequest = null;
        Features features = telemetry.getFeatures();
        if (features != null) {
            featuresRequest = new FeaturesRequest();
            featuresRequest.setWorkloadAnalytics(features.getWorkloadAnalytics());
            featuresRequest.setMonitoring(features.getMonitoring());
            setCloudStorageLoggingOnFeaturesModel(features, featuresRequest);
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
            loggingRequest.setGcs(logging.getGcs());
            loggingRequest.setStorageLocation(logging.getStorageLocation());
            loggingRequest.setEnabledSensitiveStorageLogsByEnum(logging.getEnabledSensitiveStorageLogs());
        }
        return loggingRequest;
    }

    private MonitoringRequest createMonitoringRequestFromSource(Telemetry telemetry) {
        MonitoringRequest monitoringRequest = null;
        Monitoring monitoring = telemetry.getMonitoring();
        if (monitoring != null) {
            monitoringRequest = new MonitoringRequest();
            monitoringRequest.setRemoteWriteUrl(monitoring.getRemoteWriteUrl());
        }
        return monitoringRequest;
    }

    private void setWorkloadAnalyticsFeature(Telemetry telemetry, Features features) {
        if (telemetry.getWorkloadAnalytics() != null) {
            LOGGER.debug("Setting workload analytics feature settings as workload analytics request exists.");
            features.addWorkloadAnalytics(true);
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
            logging.setGcs(loggingRequest.getGcs());
            logging.setEnabledSensitiveStorageLogsByStrings(loggingRequest.getEnabledSensitiveStorageLogs());
        }
        return logging;
    }

    private Monitoring createMonitoringFromRequest(String accountId, TelemetryRequest request) {
        Monitoring monitoring = null;
        if (entitlementService.isComputeMonitoringEnabled(accountId)) {
            monitoring = new Monitoring();
            if (request.getMonitoring() != null && StringUtils.isNotBlank(request.getMonitoring().getRemoteWriteUrl())) {
                LOGGER.debug("Create monitoring telemetry settings from monitoring request.");
                monitoring.setRemoteWriteUrl(monitoringUrlResolver.resolve(accountId, request.getMonitoring().getRemoteWriteUrl()));
            } else {
                monitoring.setRemoteWriteUrl(monitoringUrlResolver.resolve(accountId, entitlementService.isCdpSaasEnabled(accountId)));
            }
        }
        return monitoring;
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
            loggingResponse.setGcs(logging.getGcs());
            loggingResponse.setEnabledSensitiveStorageLogsByEnum(logging.getEnabledSensitiveStorageLogs());
        }
        return loggingResponse;
    }

    private MonitoringResponse createMonitoringResponseFromSource(Telemetry telemetry) {
        MonitoringResponse monitoringResponse = null;
        if (telemetry.getMonitoring() != null) {
            LOGGER.debug("Setting monitoring telemetry settings (response).");
            Monitoring monitoring = telemetry.getMonitoring();
            monitoringResponse = new MonitoringResponse();
            monitoringResponse.setRemoteWriteUrl(monitoring.getRemoteWriteUrl());
        }
        return monitoringResponse;
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
            featuresResponse.setMonitoring(features.getMonitoring());
            featuresResponse.setUseSharedAltusCredential(features.getUseSharedAltusCredential());
            setCloudStorageLoggingOnFeaturesModel(features, featuresResponse);
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
            loggingRequest.setGcs(loggingResponse.getGcs());
            loggingRequest.setEnabledSensitiveStorageLogs(loggingResponse.getEnabledSensitiveStorageLogs());
        }
        return loggingRequest;
    }

    private MonitoringRequest createMonitoringRequestFromResponse(TelemetryResponse response) {
        MonitoringRequest monitoringRequest = null;
        if (response.getMonitoring() != null) {
            LOGGER.debug("Setting monitoring response (telemetry) based on environment response");
            MonitoringResponse monitoringResponse = response.getMonitoring();
            monitoringRequest = new MonitoringRequest();
            monitoringRequest.setRemoteWriteUrl(monitoringResponse.getRemoteWriteUrl());
        }
        return monitoringRequest;
    }

    private Map<String, Object> createWorkloadAnalyticsAttributes(DetailedEnvironmentResponse environment, SdxBasicView sdxBasicView) {
        Map<String, Object> attributes = new HashMap<>();
        if (environment != null) {
            if (environment.getTelemetry() != null
                    && environment.getTelemetry().getWorkloadAnalytics() != null
                    && MapUtils.isNotEmpty(environment.getTelemetry().getWorkloadAnalytics().getAttributes())) {
                LOGGER.debug("Found environment level workload analytics attributes.");
                attributes.putAll(environment.getTelemetry().getWorkloadAnalytics().getAttributes());
            }
            if (StringUtils.isNotEmpty(environment.getCrn())) {
                attributes.put(DATABUS_HEADER_ENVIRONMENT_CRN, environment.getCrn());
            }
            if (StringUtils.isNotEmpty(environment.getName())) {
                attributes.put(DATABUS_HEADER_ENVIRONMENT_NAME, environment.getName());
            }
        }
        if (sdxBasicView != null) {
            LOGGER.debug("Adding datalake references to workload analytics attributes.");
            if (StringUtils.isNotEmpty(sdxBasicView.crn())) {
                attributes.put(DATABUS_HEADER_SDX_ID, Crn.safeFromString(sdxBasicView.crn()).getResource());
                attributes.put(DATABUS_HEADER_DATALAKE_CRN, sdxBasicView.crn());
            }
            if (StringUtils.isNotEmpty(sdxBasicView.name())) {
                attributes.put(DATABUS_HEADER_SDX_NAME, sdxBasicView.name());
                attributes.put(DATABUS_HEADER_DATALAKE_NAME, sdxBasicView.name());
            }
        }
        return attributes;
    }

    private void setMonitoring(TelemetryRequest request, Features features, String accountId) {
        if (entitlementService.isComputeMonitoringEnabled(accountId)) {
            if (request.getFeatures() != null && request.getFeatures().getMonitoring() != null) {
                LOGGER.debug("Fill cluster monitoring setting from telemetry feature request");
                features.setMonitoring(request.getFeatures().getMonitoring());
            } else {
                LOGGER.debug("Auto-filling cluster monitoring telemetry settings as it is set, but missing from the request.");
                features.addMonitoring(true);
            }
        } else {
            LOGGER.debug("Cluster monitoring feature is disabled. Set feature as false.");
            features.addMonitoring(false);
        }
    }

    private void setUseSharedAltusCredential(Features features) {
        if (useSharedAltusCredential) {
            LOGGER.debug("Fill shared altus credential setting as that is enabled globally");
            features.addUseSharedAltusCredential(true);
        }
    }

    private void setCloudStorageLogging(TelemetryRequest request, Features features) {
        if (request.getFeatures() != null && request.getFeatures().getCloudStorageLogging() != null) {
            LOGGER.debug("Fill cloud storage logging setting from feature request");
            features.setCloudStorageLogging(request.getFeatures().getCloudStorageLogging());
        } else {
            LOGGER.debug("Fill cloud storage logging setting with default value (enabled)");
            features.addCloudStorageLogging(true);
        }
    }

    private void setCloudStorageLoggingOnFeaturesModel(Features features, FeaturesBase featureModel) {
        if (features.getCloudStorageLogging() != null) {
            featureModel.setCloudStorageLogging(features.getCloudStorageLogging());
        } else {
            featureModel.addCloudStorageLogging(true);
        }
    }
}
