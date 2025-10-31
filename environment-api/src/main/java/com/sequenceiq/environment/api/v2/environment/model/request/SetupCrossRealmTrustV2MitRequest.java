package com.sequenceiq.environment.api.v2.environment.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.common.type.KdcType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SetupCrossRealmTrustV2MitRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetupCrossRealmTrustV2MitRequest extends SetupCrossRealmTrustV2KdcBaseRequest {

    @Override
    public KdcType getKdcType() {
        return KdcType.MIT;
    }

    @Override
    public String toString() {
        return "SetupCrossRealmTrustMitRequest{"
                + super.toString()
                + '}';
    }
}
