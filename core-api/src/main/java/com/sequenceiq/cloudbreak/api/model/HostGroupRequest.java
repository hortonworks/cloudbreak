package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel("HostGroupRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostGroupRequest extends HostGroupBase {

}
