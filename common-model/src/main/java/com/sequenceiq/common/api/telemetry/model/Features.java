package com.sequenceiq.common.api.telemetry.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.telemetry.base.FeaturesBase;
import com.sequenceiq.common.api.type.FeatureSetting;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Features extends FeaturesBase {

    @JsonProperty("metering")
    private FeatureSetting metering;

    public FeatureSetting getMetering() {
        return metering;
    }

    public void setMetering(FeatureSetting metering) {
        this.metering = metering;
    }
}
