package com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.base.ManagementPackV4Base;
import com.sequenceiq.cloudbreak.validation.ValidManagementPack;

import io.swagger.annotations.ApiModel;

@ApiModel
@ValidManagementPack
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ManagementPackV4Request extends ManagementPackV4Base {
}
