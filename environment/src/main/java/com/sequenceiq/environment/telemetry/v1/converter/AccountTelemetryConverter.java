package com.sequenceiq.environment.telemetry.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.environment.api.v1.telemetry.model.request.AccountTelemetryRequest;
import com.sequenceiq.environment.api.v1.telemetry.model.response.AccountTelemetryResponse;
import com.sequenceiq.environment.telemetry.domain.AccountTelemetry;

@Component
public class AccountTelemetryConverter {

    public AccountTelemetry convert(AccountTelemetryRequest request) {
        AccountTelemetry telemetry = null;
        if (request != null) {
            telemetry = new AccountTelemetry();
            telemetry.setFeatures(convertFeatures(request.getFeatures()));
            telemetry.setRules(request.getRules());
            telemetry.setEnabledSensitiveStorageLogs(request.getEnabledSensitiveStorageLogs());
        }
        return telemetry;
    }

    public AccountTelemetryResponse convert(AccountTelemetry source) {
        AccountTelemetryResponse response = null;
        if (source != null) {
            response = new AccountTelemetryResponse();
            response.setFeatures(convertFeatures(source.getFeatures()));
            response.setRules(source.getRules());
            response.setEnabledSensitiveStorageLogsByEnum(source.getEnabledSensitiveStorageLogs());
        }
        return response;
    }

    public FeaturesResponse convertFeatures(Features source) {
        FeaturesResponse response = null;
        if (source != null) {
            response = new FeaturesResponse();
            response.setMonitoring(source.getMonitoring());
            response.setWorkloadAnalytics(source.getWorkloadAnalytics());
            if (source.getCloudStorageLogging() != null) {
                response.setCloudStorageLogging(source.getCloudStorageLogging());
            } else {
                response.addCloudStorageLogging(true);
            }
        }
        return response;
    }

    public Features convertFeatures(FeaturesRequest request) {
        Features features = null;
        if (request != null) {
            features = new Features();
            features.setMonitoring(request.getMonitoring());
            features.setWorkloadAnalytics(request.getWorkloadAnalytics());
        }
        return features;
    }
}
