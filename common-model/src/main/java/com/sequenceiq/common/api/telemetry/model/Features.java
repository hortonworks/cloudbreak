package com.sequenceiq.common.api.telemetry.model;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.telemetry.base.FeaturesBase;
import com.sequenceiq.common.api.type.FeatureSetting;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Features extends FeaturesBase {

    @Deprecated
    @JsonProperty("metering")
    private FeatureSetting metering;

    @JsonProperty("monitoring")
    private FeatureSetting monitoring;

    @JsonProperty("useSharedAltusCredential")
    private FeatureSetting useSharedAltusCredential;

    @Deprecated
    public FeatureSetting getMetering() {
        return metering;
    }

    @Deprecated
    public void setMetering(FeatureSetting metering) {
        this.metering = metering;
    }

    public FeatureSetting getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(FeatureSetting monitoring) {
        this.monitoring = monitoring;
    }

    public FeatureSetting getUseSharedAltusCredential() {
        return useSharedAltusCredential;
    }

    public void setUseSharedAltusCredential(FeatureSetting useSharedAltusCredential) {
        this.useSharedAltusCredential = useSharedAltusCredential;
    }

    @JsonIgnore
    public void addMonitoring(boolean enabled) {
        monitoring = new FeatureSetting();
        monitoring.setEnabled(enabled);
    }

    @JsonIgnore
    public void addUseSharedAltusCredential(boolean enabled) {
        useSharedAltusCredential = new FeatureSetting();
        useSharedAltusCredential.setEnabled(enabled);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Features.class.getSimpleName() + "[", "]")
                .add("monitoring='" + monitoring + '\'')
                .add("useSharedAltusCredential='" + useSharedAltusCredential + '\'')
                .toString();
    }
}
