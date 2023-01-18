package com.sequenceiq.common.api.telemetry.request;

import com.sequenceiq.common.api.telemetry.base.FeaturesBase;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FeaturesRequest")
public class FeaturesRequest extends FeaturesBase {
    @Override
    public String toString() {
        return super.toString() + ", " + "FeaturesRequest{}";
    }
}
