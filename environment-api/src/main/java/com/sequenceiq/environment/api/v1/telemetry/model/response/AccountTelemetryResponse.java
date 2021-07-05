package com.sequenceiq.environment.api.v1.telemetry.model.response;

import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.environment.api.v1.telemetry.model.base.AccountTelemetryBase;

public class AccountTelemetryResponse extends AccountTelemetryBase {

    private FeaturesResponse features;

    public FeaturesResponse getFeatures() {
        return features;
    }

    public void setFeatures(FeaturesResponse features) {
        this.features = features;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "AccountTelemetryResponse{" +
                "features=" + features +
                '}';
    }
}
