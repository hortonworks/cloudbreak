package com.sequenceiq.freeipa.converter.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.TelemetryConfiguration;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.CloudwatchParams;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryConverter.class);

    private final boolean freeIpaTelemetryEnabled;

    private final boolean reportDeplymentLogs;

    private final boolean useSharedAltusCredential;

    private final String databusEndpoint;

    public TelemetryConverter(TelemetryConfiguration configuration,
            @Value("${freeipa.telemetry.enabled:true}") boolean freeIpaTelemetryEnabled) {
        this.freeIpaTelemetryEnabled = freeIpaTelemetryEnabled;
        this.reportDeplymentLogs = configuration.isReportDeploymentLogs();
        this.useSharedAltusCredential = configuration.getAltusDatabusConfiguration().isUseSharedAltusCredential();
        this.databusEndpoint = configuration.getAltusDatabusConfiguration().getAltusDatabusEndpoint();
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
            } else if (loggingRequest.getCloudwatch() != null) {
                logging.setCloudwatch(CloudwatchParams.copy(loggingRequest.getCloudwatch()));
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
            } else if (logging.getCloudwatch() != null) {
                loggingResponse.setCloudwatch(CloudwatchParams.copy(logging.getCloudwatch()));
            }
        }
        return loggingResponse;
    }

    private Features createFeaturesFromRequest(FeaturesRequest featuresRequest) {
        Features features = new Features();
        if (reportDeplymentLogs) {
            if (featuresRequest != null && featuresRequest.getReportDeploymentLogs() != null) {
                features.setReportDeploymentLogs(featuresRequest.getReportDeploymentLogs());
                LOGGER.debug("Fill report deployment log settings from feature request");
            } else {
                LOGGER.debug("Auto-fill report deployment logs settings with defaults. (disabled)");
                FeatureSetting reportDeploymentLogsFeature = new FeatureSetting();
                reportDeploymentLogsFeature.setEnabled(false);
                features.setReportDeploymentLogs(reportDeploymentLogsFeature);
            }
        }
        if (useSharedAltusCredential && featuresRequest != null
                && featuresRequest.getUseSharedAltusCredential() != null) {
            features.setUseSharedAltusCredential(featuresRequest.getUseSharedAltusCredential());
        }
        return features;
    }

    private FeaturesResponse createFeaturesResponseFromSource(Features features) {
        FeaturesResponse featuresResponse = null;
        if (features != null) {
            featuresResponse = new FeaturesResponse();
            featuresResponse.setReportDeploymentLogs(features.getReportDeploymentLogs());
            featuresResponse.setUseSharedAltusCredential(features.getUseSharedAltusCredential());
        }
        return featuresResponse;
    }

}