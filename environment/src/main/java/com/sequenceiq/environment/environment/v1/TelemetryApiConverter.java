package com.sequenceiq.environment.environment.v1;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.cloudstorage.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.request.WorkloadAnalyticsRequest;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.telemetry.response.WorkloadAnalyticsResponse;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentWorkloadAnalytics;
import com.sequenceiq.environment.environment.dto.telemetry.S3CloudStorageParameters;
import com.sequenceiq.environment.environment.dto.telemetry.WasbCloudStorageParameters;

@Component
public class TelemetryApiConverter {

    private final String databusEndpoint;

    public TelemetryApiConverter(@Value("${altus.databus.endpoint:}") String databusEndpoint) {
        this.databusEndpoint = databusEndpoint;
    }

    public EnvironmentTelemetry convert(TelemetryRequest request) {
        EnvironmentTelemetry telemetry = null;
        if (request != null) {
            EnvironmentLogging logging = null;
            EnvironmentWorkloadAnalytics workloadAnalytics = null;
            if (request.getLogging() != null) {
                LoggingRequest loggingRequest = request.getLogging();
                logging = new EnvironmentLogging();
                logging.setStorageLocation(loggingRequest.getStorageLocation());
                logging.setS3(convertS3(loggingRequest.getS3()));
                logging.setWasb(convertWasb(loggingRequest.getWasb()));
            }
            if (request.getWorkloadAnalytics() != null) {
                WorkloadAnalyticsRequest waRequest = request.getWorkloadAnalytics();
                workloadAnalytics = new EnvironmentWorkloadAnalytics();
                workloadAnalytics.setAttributes(waRequest.getAttributes());
                workloadAnalytics.setDatabusEndpoint(
                        StringUtils.isNotEmpty(waRequest.getDatabusEndpoint()) ? waRequest.getDatabusEndpoint() : databusEndpoint
                );
            }
            telemetry = new EnvironmentTelemetry(logging, workloadAnalytics);
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
            if (telemetry.getWorkloadAnalytics() != null) {
                EnvironmentWorkloadAnalytics workloadAnalytics = telemetry.getWorkloadAnalytics();
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
