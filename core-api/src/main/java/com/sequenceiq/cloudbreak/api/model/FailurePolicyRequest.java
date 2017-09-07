package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel("FailurePolicyRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FailurePolicyRequest extends FailurePolicyBase {
}
