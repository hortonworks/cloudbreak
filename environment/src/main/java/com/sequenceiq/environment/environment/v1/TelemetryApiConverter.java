package com.sequenceiq.environment.environment.v1;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.telemetry.response.WorkloadAnalyticsResponse;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.dto.telemetry.Features;
import com.sequenceiq.environment.environment.dto.telemetry.S3CloudStorageParameters;
import com.sequenceiq.environment.environment.dto.telemetry.WasbCloudStorageParameters;

@Component
public class TelemetryApiConverter {

    private final boolean reportDeploymentLogs;

    private final String databusEndpoint;

    public TelemetryApiConverter(@Value("${cluster.deployment.logs.report:false}") boolean reportDeploymentLogs,
            @Value("${altus.databus.endpoint:}") String databusEndpoint) {
        this.reportDeploymentLogs = reportDeploymentLogs;
        this.databusEndpoint = databusEndpoint;
    }

    public EnvironmentTelemetry convert(TelemetryRequest request) {
        EnvironmentTelemetry telemetry = null;
        if (request != null) {
            EnvironmentLogging logging = null;
            if (request.getLogging() != null) {
                LoggingRequest loggingRequest = request.getLogging();
                logging = new EnvironmentLogging();
                logging.setStorageLocation(loggingRequest.getStorageLocation());
                logging.setS3(convertS3(loggingRequest.getS3()));
                logging.setWasb(convertWasb(loggingRequest.getWasb()));
            }
            Map<String, Object> fluentAttributes = request.getFluentAttributes();
            Features features = null;

            if (request.getFeatures() != null) {
                features = new Features();
                FeaturesRequest featuresRequest = request.getFeatures();
                if (reportDeploymentLogs) {
                    final FeatureSetting reportDeploymentLogs;
                    if (featuresRequest.getReportDeploymentLogs() != null) {
                        reportDeploymentLogs = featuresRequest.getReportDeploymentLogs();
                    } else {
                        reportDeploymentLogs = new FeatureSetting();
                        reportDeploymentLogs.setEnabled(false);
                    }
                    features.setReportDeploymentLogs(reportDeploymentLogs);
                }
            }
            telemetry = new EnvironmentTelemetry(logging, features, fluentAttributes, databusEndpoint);
        }
        return telemetry;
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

    private WasbCloudStorageParameters convertWasb(WasbCloudStorageV1Parameters wasb) {
        WasbCloudStorageParameters wasbCloudStorageParameters = null;
        if (wasb != null) {
            wasbCloudStorageParameters = new WasbCloudStorageParameters();
            wasbCloudStorageParameters.setAccountKey(wasb.getAccountKey());
            wasbCloudStorageParameters.setAccountName(wasb.getAccountName());
            wasbCloudStorageParameters.setSecure(wasb.isSecure());
        }
        return wasbCloudStorageParameters;
    }

    public TelemetryResponse convert(EnvironmentTelemetry telemetry) {
        TelemetryResponse response = null;
        if (telemetry != null) {
            LoggingResponse loggingResponse = null;
            WorkloadAnalyticsResponse waResponse = null;
            if (telemetry.getLogging() != null) {
                EnvironmentLogging logging = telemetry.getLogging();
                loggingResponse = new LoggingResponse();
                loggingResponse.setStorageLocation(logging.getStorageLocation());
                loggingResponse.setS3(convertS3(logging.getS3()));
                loggingResponse.setWasb(convertWasb(logging.getWasb()));
            }
            response = new TelemetryResponse();
            response.setLogging(loggingResponse);
            response.setWorkloadAnalytics(waResponse);
            response.setFluentAttributes(telemetry.getFluentAttributes());
            if (telemetry.getFeatures() != null) {
                FeaturesResponse featuresResponse = new FeaturesResponse();
                featuresResponse.setReportDeploymentLogs(telemetry.getFeatures().getReportDeploymentLogs());
                response.setFeatures(featuresResponse);
            }
            response.setFluentAttributes(telemetry.getFluentAttributes());
            response.setDatabusEndpoint(databusEndpoint);
        }
        return response;
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

    private WasbCloudStorageV1Parameters convertWasb(WasbCloudStorageParameters wasb) {
        WasbCloudStorageV1Parameters s3CloudStorageV1Parameters = null;
        if (wasb != null) {
            s3CloudStorageV1Parameters = new WasbCloudStorageV1Parameters();
            s3CloudStorageV1Parameters.setAccountKey(wasb.getAccountKey());
            s3CloudStorageV1Parameters.setAccountName(wasb.getAccountName());
            s3CloudStorageV1Parameters.setSecure(wasb.isSecure());
        }
        return s3CloudStorageV1Parameters;
    }
}
