package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(subTypes = {EnvironmentAttachRequest.class, EnvironmentDetachRequest.class, EnvironmentRequest.class})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class EnvironmentBaseRequest implements Serializable {

    @Override
    public String toString() {
        return "EnvironmentBaseRequest{}";
    }
}
