package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateTrustedRealmRequest implements JsonEntity {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String realm;

    private boolean saltUpdateRequired;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public boolean isSaltUpdateRequired() {
        return saltUpdateRequired;
    }

    public void setSaltUpdateRequired(boolean saltUpdateRequired) {
        this.saltUpdateRequired = saltUpdateRequired;
    }

    @Override
    public String toString() {
        return "UpdateTrustedRealmRequest{" +
                "realm='" + realm + '\'' +
                ", saltUpdateRequired=" + saltUpdateRequired +
                '}';
    }
}


