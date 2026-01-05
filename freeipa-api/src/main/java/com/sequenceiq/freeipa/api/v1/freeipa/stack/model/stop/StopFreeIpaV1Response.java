package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.stop;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.BaseFlowIdentifierResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StopFreeIpaV1Response extends BaseFlowIdentifierResponse {

    @Override
    public String toString() {
        return "StopFreeIpaV1Response{" +
                super.toString() +
                '}';
    }
}
