package com.sequenceiq.environment.api.v1.telemetry.model.request;

import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.environment.api.v1.telemetry.model.base.AccountTelemetryBase;

public class AccountTelemetryRequest extends AccountTelemetryBase {

    private FeaturesRequest features;

    public FeaturesRequest getFeatures() {
        return features;
    }

    public void setFeatures(FeaturesRequest features) {
        this.features = features;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "AccountTelemetryRequest{" +
                "features=" + features +
                '}';
    }
}
