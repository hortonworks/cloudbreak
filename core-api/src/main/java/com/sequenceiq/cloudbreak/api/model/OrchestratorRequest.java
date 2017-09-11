package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel("OrchestratorRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrchestratorRequest extends OrchestratorBase {
}
