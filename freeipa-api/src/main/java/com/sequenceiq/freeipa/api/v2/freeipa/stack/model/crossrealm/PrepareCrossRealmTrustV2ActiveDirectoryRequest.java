package com.sequenceiq.freeipa.api.v2.freeipa.stack.model.crossrealm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.common.type.KdcType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PrepareCrossRealmTrustV2ActiveDirectoryRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrepareCrossRealmTrustV2ActiveDirectoryRequest extends PrepareCrossRealmTrustV2KdcBaseRequest {

    @Override
    public KdcType getKdcType() {
        return KdcType.ACTIVE_DIRECTORY;
    }
}
