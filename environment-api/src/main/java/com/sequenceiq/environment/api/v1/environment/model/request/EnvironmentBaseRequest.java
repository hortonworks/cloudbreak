package com.sequenceiq.environment.api.v1.environment.model.request;

import com.sequenceiq.environment.api.v1.environment.model.request.operations.EnvironmentAttachRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.operations.EnvironmentDetachRequest;

import io.swagger.annotations.ApiModel;

@ApiModel(subTypes = {EnvironmentAttachRequest.class, EnvironmentDetachRequest.class, EnvironmentRequest.class})
public abstract class EnvironmentBaseRequest {

}
