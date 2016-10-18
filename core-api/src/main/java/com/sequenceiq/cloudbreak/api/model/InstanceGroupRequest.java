package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "instanceGroups")
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstanceGroupRequest extends InstanceGroupBase {

}
