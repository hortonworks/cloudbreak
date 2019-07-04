package com.sequenceiq.environment.api.v1.environment.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.environment.api.v1.environment.model.common.EnvironmentNetworkBase;

import io.swagger.annotations.ApiModel;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "EnvironmentNetworkV1Request")
public class EnvironmentNetworkRequest extends EnvironmentNetworkBase {
}
