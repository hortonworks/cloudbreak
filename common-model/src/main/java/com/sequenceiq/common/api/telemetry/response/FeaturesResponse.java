package com.sequenceiq.common.api.telemetry.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.telemetry.base.FeaturesBase;
import com.sequenceiq.common.api.telemetry.doc.TelemetryModelDescription;
import com.sequenceiq.common.api.type.FeatureSetting;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FeaturesResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeaturesResponse extends FeaturesBase {

    @Deprecated
    @JsonProperty("metering")
    @Schema(description = TelemetryModelDescription.TELEMETRY_METERING)
    private FeatureSetting metering;

    @JsonProperty("useSharedAltusCredential")
    @Schema(description = TelemetryModelDescription.TELEMETRY_USE_SHARED_ALTUS_CREDENTIAL_ENABLED)
    private FeatureSetting useSharedAltusCredential;

    @Deprecated
    public FeatureSetting getMetering() {
        return metering;
    }

    @Deprecated
    public void setMetering(FeatureSetting metering) {
        this.metering = metering;
    }

    public FeatureSetting getUseSharedAltusCredential() {
        return useSharedAltusCredential;
    }

    public void setUseSharedAltusCredential(FeatureSetting useSharedAltusCredential) {
        this.useSharedAltusCredential = useSharedAltusCredential;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "FeaturesResponse{" +
                ", useSharedAltusCredential=" + useSharedAltusCredential +
                '}';
    }
}
