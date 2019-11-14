package com.sequenceiq.freeipa.converter.telemetry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.FeatureSetting;

@Component
public class TelemetryConverter {

    private final boolean freeIpaTelemetryEnabled;

    private final boolean reportDeplymentLogs;

    private final String databusEndpoint;

    public TelemetryConverter(@Value("${freeipa.telemetry.enabled:false}") boolean freeIpaTelemetryEnabled,
            @Value("${cluster.deployment.logs.report:false}") boolean reportDeplymentLogs,
            @Value("${altus.databus.endpoint:}") String databusEndpoint) {
        this.freeIpaTelemetryEnabled = freeIpaTelemetryEnabled;
        this.reportDeplymentLogs = reportDeplymentLogs;
        this.databusEndpoint = databusEndpoint;
    }

    public Telemetry convert(TelemetryRequest request) {
        Telemetry telemetry = null;
        if (freeIpaTelemetryEnabled && request != null) {
            telemetry = new Telemetry();
            telemetry.setLogging(createLoggingFromRequest(request.getLogging()));
            telemetry.setFeatures(createFeaturesFromRequest(request.getFeatures()));
            telemetry.setDatabusEndpoint(databusEndpoint);
            telemetry.setFluentAttributes(request.getFluentAttributes());
        }
        return telemetry;
    }

    public TelemetryResponse convert(Telemetry telemetry) {
        TelemetryResponse response = null;
        if (freeIpaTelemetryEnabled && telemetry != null) {
            response = new TelemetryResponse();
            response.setFluentAttributes(telemetry.getFluentAttributes());
            response.setLogging(createLoggingResponseFromSource(telemetry.getLogging()));
            response.setFeatures(createFeaturesResponseFromSource(telemetry.getFeatures()));
        }
        return response;
    }

    private Logging createLoggingFromRequest(LoggingRequest loggingRequest) {
        Logging logging = null;
        if (loggingRequest != null) {
            logging = new Logging();
            logging.setStorageLocation(loggingRequest.getStorageLocation());
            if (loggingRequest.getS3() != null) {
                S3CloudStorageV1Parameters s3Params = new S3CloudStorageV1Parameters();
                s3Params.setInstanceProfile(loggingRequest.getS3().getInstanceProfile());
                logging.setS3(s3Params);
            } else if (loggingRequest.getAdlsGen2() != null) {
                AdlsGen2CloudStorageV1Parameters adlsGen2Params = new AdlsGen2CloudStorageV1Parameters();
                AdlsGen2CloudStorageV1Parameters adlsGen2FromRequest = loggingRequest.getAdlsGen2();
                adlsGen2Params.setAccountKey(adlsGen2FromRequest.getAccountKey());
                adlsGen2Params.setAccountName(adlsGen2FromRequest.getAccountName());
                adlsGen2Params.setSecure(adlsGen2FromRequest.isSecure());
                adlsGen2Params.setManagedIdentity(adlsGen2FromRequest.getManagedIdentity());
                logging.setAdlsGen2(adlsGen2Params);
            }
        }
        return logging;
    }

    private LoggingResponse createLoggingResponseFromSource(Logging logging) {
        LoggingResponse loggingResponse = null;
        if (logging != null) {
            loggingResponse = new LoggingResponse();
            loggingResponse.setStorageLocation(logging.getStorageLocation());
            if (logging.getS3() != null) {
                S3CloudStorageV1Parameters s3Params = new S3CloudStorageV1Parameters();
                s3Params.setInstanceProfile(logging.getS3().getInstanceProfile());
                loggingResponse.setS3(s3Params);
            } else if (logging.getAdlsGen2() != null) {
                AdlsGen2CloudStorageV1Parameters adlsGen2Params = new AdlsGen2CloudStorageV1Parameters();
                adlsGen2Params.setAccountKey(logging.getAdlsGen2().getAccountKey());
                adlsGen2Params.setAccountName(logging.getAdlsGen2().getAccountName());
                adlsGen2Params.setSecure(logging.getAdlsGen2().isSecure());
                adlsGen2Params.setManagedIdentity(logging.getAdlsGen2().getManagedIdentity());
                loggingResponse.setAdlsGen2(adlsGen2Params);
            }
        }
        return loggingResponse;
    }

    private Features createFeaturesFromRequest(FeaturesRequest featuresRequest) {
        Features features = null;
        if (reportDeplymentLogs) {
            features = new Features();
            if (featuresRequest != null && featuresRequest.getReportDeploymentLogs() != null) {
                features.setReportDeploymentLogs(featuresRequest.getReportDeploymentLogs());
            } else {
                FeatureSetting reportDeploymentLogsFeature = new FeatureSetting();
                reportDeploymentLogsFeature.setEnabled(true);
                features.setReportDeploymentLogs(reportDeploymentLogsFeature);
            }
        }
        return features;
    }

    private FeaturesResponse createFeaturesResponseFromSource(Features features) {
        FeaturesResponse featuresResponse = null;
        if (features != null && features.getReportDeploymentLogs() != null) {
            featuresResponse = new FeaturesResponse();
            featuresResponse.setReportDeploymentLogs(features.getReportDeploymentLogs());
        }
        return featuresResponse;
    }

}