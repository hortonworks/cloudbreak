package com.sequenceiq.environment.environment.v1;

import java.util.HashMap;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.TelemetryConfiguration;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.CloudwatchParams;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.telemetry.response.WorkloadAnalyticsResponse;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentWorkloadAnalytics;
import com.sequenceiq.environment.environment.dto.telemetry.S3CloudStorageParameters;

@Component
public class TelemetryApiConverter {

    private final boolean reportDeploymentLogs;

    private final boolean useSharedAltusCredential;

    public TelemetryApiConverter(TelemetryConfiguration configuration) {
        this.reportDeploymentLogs = configuration.isReportDeploymentLogs();
        this.useSharedAltusCredential = configuration.getAltusDatabusConfiguration().isUseSharedAltusCredential();
    }

    public EnvironmentTelemetry convert(TelemetryRequest request) {
        EnvironmentTelemetry telemetry = null;
        if (request != null) {
            telemetry = new EnvironmentTelemetry();
            telemetry.setLogging(createLoggingFromRequest(request.getLogging()));
            telemetry.setFeatures(createEnvironmentFeaturesFromRequest(request.getFeatures()));
            telemetry.setFluentAttributes(new HashMap<>(request.getFluentAttributes()));
        }
        return telemetry;
    }

    public TelemetryResponse convert(EnvironmentTelemetry telemetry) {
        TelemetryResponse response = null;
        if (telemetry != null) {
            response = new TelemetryResponse();
            response.setLogging(createLoggingResponseFromSource(telemetry.getLogging()));
            response.setWorkloadAnalytics(createWorkloadAnalyticsResponseFromSource(telemetry.getWorkloadAnalytics()));
            response.setFluentAttributes(telemetry.getFluentAttributes());
            response.setFeatures(createFeaturesResponseFromSource(telemetry.getFeatures()));
            response.setFluentAttributes(telemetry.getFluentAttributes());
        }
        return response;
    }

    public TelemetryRequest convertToRequest(EnvironmentTelemetry telemetry) {
        TelemetryRequest telemetryRequest = null;
        if (telemetry != null) {
            telemetryRequest = new TelemetryRequest();
            telemetryRequest.setFluentAttributes(telemetry.getFluentAttributes());
            telemetryRequest.setLogging(createLoggingRequestFromEnvSource(telemetry.getLogging()));
            telemetryRequest.setFeatures(createFeaturesRequestEnvSource(telemetry.getFeatures()));
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
            loggingRequest.setCloudwatch(CloudwatchParams.copy(logging.getCloudwatch()));
        }
        return loggingRequest;
    }

    private FeaturesRequest createFeaturesRequestEnvSource(EnvironmentFeatures features) {
        FeaturesRequest featuresRequest = null;
        if (features != null) {
            featuresRequest = new FeaturesRequest();
            featuresRequest.setReportDeploymentLogs(features.getReportDeploymentLogs());
        }
        return featuresRequest;
    }

    private FeaturesResponse createFeaturesResponseFromSource(EnvironmentFeatures features) {
        FeaturesResponse featuresResponse = null;
        if (features != null) {
            featuresResponse = new FeaturesResponse();
            featuresResponse.setReportDeploymentLogs(features.getReportDeploymentLogs());
            featuresResponse.setWorkloadAnalytics(features.getWorkloadAnalytics());
            featuresResponse.setUseSharedAltusCredential(features.getUseSharedAltusCredential());
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
            logging.setCloudwatch(CloudwatchParams.copy(loggingRequest.getCloudwatch()));
        }
        return logging;
    }

    private EnvironmentFeatures createEnvironmentFeaturesFromRequest(FeaturesRequest featuresRequest) {
        EnvironmentFeatures features = null;
        if (featuresRequest != null) {
            features = new EnvironmentFeatures();
            if (useSharedAltusCredential) {
                features.addUseSharedAltusredential(true);
            }
            if (reportDeploymentLogs) {
                if (featuresRequest.getReportDeploymentLogs() != null) {
                    features.setReportDeploymentLogs(featuresRequest.getReportDeploymentLogs());
                } else {
                    features.addReportDeploymentLogs(false);
                }
            }
            if (featuresRequest.getWorkloadAnalytics() != null) {
                features.setWorkloadAnalytics(featuresRequest.getWorkloadAnalytics());
            }
        }
        return features;
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
            loggingResponse.setCloudwatch(CloudwatchParams.copy(logging.getCloudwatch()));
        }
        return loggingResponse;
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
}