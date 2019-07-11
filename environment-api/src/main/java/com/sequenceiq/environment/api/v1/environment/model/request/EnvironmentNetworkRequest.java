package com.sequenceiq.environment.api.v1.environment.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.v1.environment.model.base.EnvironmentNetworkBase;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "EnvironmentNetworkV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EnvironmentNetworkRequest extends EnvironmentNetworkBase {
}
