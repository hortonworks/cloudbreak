package com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ModifyProxyConfigSaltStateApplyRequest extends ModifyProxyConfigRequest {

    @JsonCreator
    public ModifyProxyConfigSaltStateApplyRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("previousProxyConfigCrn") String previousProxyConfigCrn) {
        super(null, stackId, previousProxyConfigCrn);
    }
}
