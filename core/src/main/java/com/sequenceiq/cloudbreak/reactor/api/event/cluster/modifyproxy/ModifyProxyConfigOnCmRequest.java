package com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ModifyProxyConfigOnCmRequest extends ModifyProxyConfigRequest {

    @JsonCreator
    public ModifyProxyConfigOnCmRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("previousProxyConfigCrn") String previousProxyConfigCrn) {
        super(null, stackId, previousProxyConfigCrn);
    }
}
