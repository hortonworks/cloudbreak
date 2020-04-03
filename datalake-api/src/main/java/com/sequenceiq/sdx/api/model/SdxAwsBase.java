package com.sequenceiq.sdx.api.model;

import javax.validation.Valid;

public class SdxAwsBase {

    @Valid
    private SdxAwsSpotParameters spot;

    public SdxAwsSpotParameters getSpot() {
        return spot;
    }

    public void setSpot(SdxAwsSpotParameters spot) {
        this.spot = spot;
    }
}
