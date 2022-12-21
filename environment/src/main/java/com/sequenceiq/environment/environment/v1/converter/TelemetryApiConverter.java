package com.sequenceiq.environment.environment.v1.converter;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfiguration;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.base.FeaturesBase;
import com.sequenceiq.common.api.telemetry.model.CloudwatchParams;
import com.sequenceiq.common.api.telemetry.model.Features;
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
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentMonitoring;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentWorkloadAnalytics;
import com.sequenceiq.environment.environment.dto.telemetry.S3CloudStorageParameters;

@Component
public class TelemetryApiConverter {

    private static final String ACCOUNT_ID_TEMPLATE = "$accountid";

    private final boolean clusterLogsCollection;

    private final boolean useSharedAltusCredential;

    private final String monitoringEndpointConfig;

    private final String monitoringPaasEndpointConfig;

    private final EntitlementService entitlementService;

    public TelemetryApiConverter(TelemetryConfiguration configuration, EntitlementService entitlementService) {
        this.clusterLogsCollection = configuration.getClusterLogsCollectionConfiguration().isEnabled();
        this.entitlementService = entitlementService;
        this.monitoringEndpointConfig = configuration.getMonitoringConfiguration().getRemoteWriteUrl();
        this.monitoringPaasEndpointConfig = configuration.getMonitoringConfiguration().getPaasRemoteWriteUrl();
        this.useSharedAltusCredential = configuration.getAltusDatabusConfiguration().isUseSharedAltusCredential();
    }

    public EnvironmentTelemetry convert(TelemetryRequest request, Features accountFeatures, String accountId) {
        EnvironmentTelemetry telemetry = null;
        if (request != null) {
            telemetry = new EnvironmentTelemetry();
            telemetry.setLogging(createLoggingFromRequest(request.getLogging()));
            telemetry.setMonitoring(createMonitoringFromRequest(request.getMonitoring(), accountId));
            telemetry.setWorkloadAnalytics(createWorkloadAnalyticsFromRequest(request.getWorkloadAnalytics()));
            telemetry.setFeatures(createEnvironmentFeaturesFromRequest(request.getFeatures(), accountFeatures, accountId));
            telemetry.setFluentAttributes(new HashMap<>(request.getFluentAttributes()));
        }
        return telemetry;
    }

    public TelemetryResponse convert(EnvironmentTelemetry telemetry, String accountId) {
        TelemetryResponse response = null;
        if (telemetry != null) {
            response = new TelemetryResponse();
            response.setLogging(createLoggingResponseFromSource(telemetry.getLogging()));
            response.setMonitoring(createMonitoringResponseFromSource(telemetry.getMonitoring()));
            response.setWorkloadAnalytics(createWorkloadAnalyticsResponseFromSource(telemetry.getWorkloadAnalytics()));
            response.setFluentAttributes(telemetry.getFluentAttributes());
            response.setFeatures(createFeaturesResponseFromSource(telemetry.getFeatures(), accountId));
            response.setFluentAttributes(telemetry.getFluentAttributes());
        }
        return response;
    }

    public TelemetryRequest convertToRequest(EnvironmentTelemetry telemetry, String accountId) {
        TelemetryRequest telemetryRequest = null;
        if (telemetry != null) {
            telemetryRequest = new TelemetryRequest();
            telemetryRequest.setFluentAttributes(telemetry.getFluentAttributes());
            telemetryRequest.setLogging(createLoggingRequestFromEnvSource(telemetry.getLogging()));
            telemetryRequest.setMonitoring(createMonitoringRequestFromEnvSource(telemetry.getMonitoring()));
            telemetryRequest.setFeatures(createFeaturesRequestEnvSource(telemetry.getFeatures(), accountId));
        }
        return telemetryRequest;
    }

    private LoggingRequest createLoggingRequestFromEnvSource(EnvironmentLogging logging) {
        LoggingRequest loggingRequest = null;
        if (logging != null) {
            loggingRequest = new LoggingRequest();
            loggingRequest.setStorageLocation(logging.getStorageLocation());
            loggingRequest.setS3(convertS3(logging.getS3()));
            loggingRequest.setAdlsGen2(convertAdlsV2(logging.getAdlsGen2()));
            loggingRequest.setGcs(convertGcs(logging.getGcs()));
            loggingRequest.setCloudwatch(CloudwatchParams.copy(logging.getCloudwatch()));
        }
        return loggingRequest;
    }

    private MonitoringRequest createMonitoringRequestFromEnvSource(EnvironmentMonitoring monitoring) {
        MonitoringRequest monitoringRequest = null;
        if (monitoring != null) {
            monitoringRequest = new MonitoringRequest();
            monitoringRequest.setRemoteWriteUrl(monitoring.getRemoteWriteUrl());
        }
        return monitoringRequest;
    }

    private FeaturesRequest createFeaturesRequestEnvSource(EnvironmentFeatures features, String accountId) {
        FeaturesRequest featuresRequest = null;
        if (features != null) {
            featuresRequest = new FeaturesRequest();
            featuresRequest.setClusterLogsCollection(features.getClusterLogsCollection());
            featuresRequest.setMonitoring(features.getMonitoring());
            setCloudStorageLoggingOnFeaturesModel(features, featuresRequest);
            setMonitoringOnFeaturesModel(features, featuresRequest, accountId);
        }
        return featuresRequest;
    }

    private FeaturesResponse createFeaturesResponseFromSource(EnvironmentFeatures features, String accountId) {
        FeaturesResponse featuresResponse = null;
        if (features != null) {
            featuresResponse = new FeaturesResponse();
            featuresResponse.setClusterLogsCollection(features.getClusterLogsCollection());
            featuresResponse.setWorkloadAnalytics(features.getWorkloadAnalytics());
            featuresResponse.setUseSharedAltusCredential(features.getUseSharedAltusCredential());
            setCloudStorageLoggingOnFeaturesModel(features, featuresResponse);
            setMonitoringOnFeaturesModel(features, featuresResponse, accountId);
        }
        return featuresResponse;
    }

    private EnvironmentLogging createLoggingFromRequest(LoggingRequest loggingRequest) {
        EnvironmentLogging logging = null;
        if (loggingRequest != null) {
            logging = new EnvironmentLogging();
            logging.setStorageLocation(loggingRequest.getStorageLocation());
            logging.setS3(convertS3(loggingRequest.getS3()));
            logging.setAdlsGen2(convertAdlsV2(loggingRequest.getAdlsGen2()));
            logging.setGcs(convertGcs(loggingRequest.getGcs()));
            logging.setCloudwatch(CloudwatchParams.copy(loggingRequest.getCloudwatch()));
        }
        return logging;
    }

    private EnvironmentMonitoring createMonitoringFromRequest(MonitoringRequest monitoringRequest, String accountId) {
        EnvironmentMonitoring monitoring = new EnvironmentMonitoring();
        if (entitlementService.isComputeMonitoringEnabled(accountId)) {
            String unformattedMonitoringEndpoint = getUnformattedMonitoringEndpoint(accountId);
            if (monitoringRequest != null) {
                String preEndpoint = StringUtils.isNotBlank(monitoringRequest.getRemoteWriteUrl())
                        ? monitoringRequest.getRemoteWriteUrl() : unformattedMonitoringEndpoint;
                monitoring.setRemoteWriteUrl(replaceAccountId(preEndpoint, accountId));
            } else {
                monitoring.setRemoteWriteUrl(replaceAccountId(unformattedMonitoringEndpoint, accountId));
            }
        }
        return monitoring;
    }

    private EnvironmentFeatures createEnvironmentFeaturesFromRequest(FeaturesRequest featuresRequest, Features accountFeatures, String accountId) {
        EnvironmentFeatures features = null;
        if (featuresRequest != null) {
            features = new EnvironmentFeatures();
            if (useSharedAltusCredential) {
                features.addUseSharedAltusredential(true);
            }
            setClusterLogsCollectionFromAccountAndRequest(featuresRequest, accountFeatures, features);
            setMonitoringFromAccountAndRequest(featuresRequest, accountFeatures, features, accountId);
            setCloudStorageLoggingFromAccountAndRequest(featuresRequest, accountFeatures, features);
            if (accountFeatures.getWorkloadAnalytics() != null) {
                features.setWorkloadAnalytics(accountFeatures.getWorkloadAnalytics());
            }
            if (featuresRequest.getWorkloadAnalytics() != null) {
                features.setWorkloadAnalytics(featuresRequest.getWorkloadAnalytics());
            }
        }
        return features;
    }

    private void setCloudStorageLoggingFromAccountAndRequest(FeaturesRequest featuresRequest, Features accountFeatures, EnvironmentFeatures features) {
        if (accountFeatures.getCloudStorageLogging() != null) {
            features.setCloudStorageLogging(accountFeatures.getCloudStorageLogging());
        }
        if (featuresRequest.getCloudStorageLogging() != null) {
            features.setCloudStorageLogging(featuresRequest.getCloudStorageLogging());
        } else {
            features.addCloudStorageLogging(true);
        }
    }

    private void setClusterLogsCollectionFromAccountAndRequest(FeaturesRequest featuresRequest, Features accountFeatures, EnvironmentFeatures features) {
        if (clusterLogsCollection) {
            if (accountFeatures.getClusterLogsCollection() != null) {
                features.setClusterLogsCollection(accountFeatures.getClusterLogsCollection());
            }
            if (featuresRequest.getClusterLogsCollection() != null) {
                features.setClusterLogsCollection(featuresRequest.getClusterLogsCollection());
            } else {
                features.addClusterLogsCollection(false);
            }
        }
    }

    private void setMonitoringFromAccountAndRequest(FeaturesRequest featuresRequest, Features accountFeatures, EnvironmentFeatures features, String accountId) {
        if (entitlementService.isComputeMonitoringEnabled(accountId)) {
            if (accountFeatures.getMonitoring() != null) {
                features.setMonitoring(accountFeatures.getMonitoring());
            }
            if (featuresRequest.getMonitoring() != null) {
                features.setMonitoring(featuresRequest.getMonitoring());
            } else {
                features.addMonitoring(true);
            }
        }
    }

    private void setMonitoringOnFeaturesModel(EnvironmentFeatures features, FeaturesBase featureModel, String accountId) {
        if (entitlementService.isComputeMonitoringEnabled(accountId)) {
            if (features.getMonitoring() != null) {
                featureModel.setMonitoring(features.getMonitoring());
            } else {
                featureModel.addMonitoring(true);
            }
        }
    }

    private void setCloudStorageLoggingOnFeaturesModel(EnvironmentFeatures features, FeaturesBase featureModel) {
        if (features.getCloudStorageLogging() != null) {
            featureModel.setCloudStorageLogging(features.getCloudStorageLogging());
        } else {
            featureModel.addCloudStorageLogging(true);
        }
    }

    private EnvironmentWorkloadAnalytics createWorkloadAnalyticsFromRequest(WorkloadAnalyticsRequest request) {
        EnvironmentWorkloadAnalytics wa = null;
        if (request != null) {
            wa = new EnvironmentWorkloadAnalytics();
            wa.setAttributes(request.getAttributes());
        }
        return wa;
    }

    private S3CloudStorageParameters convertS3(S3CloudStorageV1Parameters s3) {
        S3CloudStorageParameters s3CloudStorageParameters = null;
        if (s3 != null) {
            s3CloudStorageParameters = new S3CloudStorageParameters();
            s3CloudStorageParameters.setInstanceProfile(s3.getInstanceProfile());
            return s3CloudStorageParameters;
        }
        return s3CloudStorageParameters;
    }

    private WorkloadAnalyticsResponse createWorkloadAnalyticsResponseFromSource(EnvironmentWorkloadAnalytics workloadAnalytics) {
        WorkloadAnalyticsResponse waResponse = null;
        if (workloadAnalytics != null) {
            waResponse = new WorkloadAnalyticsResponse();
            waResponse.setAttributes(workloadAnalytics.getAttributes());
        }
        return waResponse;
    }

    private LoggingResponse createLoggingResponseFromSource(EnvironmentLogging logging) {
        LoggingResponse loggingResponse = null;
        if (logging != null) {
            loggingResponse = new LoggingResponse();
            loggingResponse.setStorageLocation(logging.getStorageLocation());
            loggingResponse.setS3(convertS3(logging.getS3()));
            loggingResponse.setAdlsGen2(convertAdlsV2(logging.getAdlsGen2()));
            loggingResponse.setGcs(convertGcs(logging.getGcs()));
            loggingResponse.setCloudwatch(CloudwatchParams.copy(logging.getCloudwatch()));
        }
        return loggingResponse;
    }

    private MonitoringResponse createMonitoringResponseFromSource(EnvironmentMonitoring monitoring) {
        MonitoringResponse monitoringResponse = null;
        if (monitoring != null) {
            monitoringResponse = new MonitoringResponse();
            monitoringResponse.setRemoteWriteUrl(monitoring.getRemoteWriteUrl());
        }
        return monitoringResponse;
    }

    private S3CloudStorageV1Parameters convertS3(S3CloudStorageParameters s3) {
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = null;
        if (s3 != null) {
            s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
            s3CloudStorageV1Parameters.setInstanceProfile(s3.getInstanceProfile());
            return s3CloudStorageV1Parameters;
        }
        return s3CloudStorageV1Parameters;
    }

    private AdlsGen2CloudStorageV1Parameters convertAdlsV2(AdlsGen2CloudStorageV1Parameters adlsV2) {
        AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageV1Parameters = null;
        if (adlsV2 != null) {
            adlsGen2CloudStorageV1Parameters = new AdlsGen2CloudStorageV1Parameters();
            adlsGen2CloudStorageV1Parameters.setAccountKey(adlsV2.getAccountKey());
            adlsGen2CloudStorageV1Parameters.setAccountName(adlsV2.getAccountName());
            adlsGen2CloudStorageV1Parameters.setManagedIdentity(adlsV2.getManagedIdentity());
            adlsGen2CloudStorageV1Parameters.setSecure(adlsV2.isSecure());
        }
        return adlsGen2CloudStorageV1Parameters;
    }

    private GcsCloudStorageV1Parameters convertGcs(GcsCloudStorageV1Parameters gcs) {
        GcsCloudStorageV1Parameters gcsCloudStorageV1Parameters = null;
        if (gcs != null) {
            gcsCloudStorageV1Parameters = new GcsCloudStorageV1Parameters();
            gcsCloudStorageV1Parameters.setServiceAccountEmail(gcs.getServiceAccountEmail());
        }
        return gcsCloudStorageV1Parameters;
    }

    private String replaceAccountId(String endpoint, String accountId) {
        if (StringUtils.isNoneBlank(endpoint, accountId)) {
            endpoint = endpoint.replace(ACCOUNT_ID_TEMPLATE, accountId);
        }
        return endpoint;
    }

    private String getUnformattedMonitoringEndpoint(String accountId) {
        return entitlementService.isCdpSaasEnabled(accountId) || StringUtils.isBlank(monitoringPaasEndpointConfig)
                ? monitoringEndpointConfig : monitoringPaasEndpointConfig;
    }
}