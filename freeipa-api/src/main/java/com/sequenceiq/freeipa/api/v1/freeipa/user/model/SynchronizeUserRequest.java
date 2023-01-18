package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SynchronizeUserV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SynchronizeUserRequest  extends SynchronizeOperationRequestBase {
    public SynchronizeUserRequest() {
    }

    public SynchronizeUserRequest(Set<String> environments) {
        super(environments);
    }

    @Override
    public String toString() {
        return "SynchronizeUserRequest{"
                + super.fieldsToString()
                + "}";
    }
}
