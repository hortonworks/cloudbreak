package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FinishCrossRealmTrustRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FinishCrossRealmTrustRequest extends FinishCrossRealmTrustBase {
    @Override
    public String toString() {
        return "FinishCrossRealmTrustRequest{} " + super.toString();
    }
}
