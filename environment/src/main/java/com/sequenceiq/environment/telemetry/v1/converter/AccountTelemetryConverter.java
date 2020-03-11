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
        }
        return telemetry;
    }

    public AccountTelemetryResponse convert(AccountTelemetry source) {
        AccountTelemetryResponse response = null;
        if (source != null) {
            response = new AccountTelemetryResponse();
            response.setFeatures(convertFeatures(source.getFeatures()));
            response.setRules(source.getRules());
        }
        return response;
    }

    public FeaturesResponse convertFeatures(Features source) {
        FeaturesResponse response = null;
        if (source != null) {
            response = new FeaturesResponse();
            response.setClusterLogsCollection(source.getClusterLogsCollection());
            response.setWorkloadAnalytics(source.getWorkloadAnalytics());
        }
        return response;
    }

    public Features convertFeatures(FeaturesRequest request) {
        Features features = null;
        if (request != null) {
            features = new Features();
            features.setClusterLogsCollection(request.getClusterLogsCollection());
            features.setWorkloadAnalytics(request.getWorkloadAnalytics());
        }
        return features;
    }
}
