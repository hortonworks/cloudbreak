package com.sequenceiq.freeipa.api.v1.freeipa.test.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ClientTestBaseV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientTestBaseV1Response {

    @Schema(description = ModelDescriptions.RESULT, requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean result = Boolean.FALSE;

    public ClientTestBaseV1Response() {
    }

    public ClientTestBaseV1Response(Boolean result) {
        this.result = result;
    }

    public static ClientTestBaseV1Response resultOf(Boolean result) {
        return new ClientTestBaseV1Response(result);
    }

    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }
}
