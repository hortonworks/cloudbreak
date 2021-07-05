package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;

@ApiModel(subTypes = {EnvironmentAttachRequest.class, EnvironmentDetachRequest.class, EnvironmentRequest.class})
public abstract class EnvironmentBaseRequest implements Serializable {

    @Override
    public String toString() {
        return "EnvironmentBaseRequest{}";
    }
}
