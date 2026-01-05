package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.start;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.BaseFlowIdentifierResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StartFreeIpaV1Response extends BaseFlowIdentifierResponse {

    @Override
    public String toString() {
        return "StartFreeIpaV1Response{" +
                super.toString() +
                '}';
    }
}
