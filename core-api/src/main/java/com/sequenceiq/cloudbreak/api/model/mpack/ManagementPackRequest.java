package com.sequenceiq.cloudbreak.api.model.mpack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.validation.ValidManagementPack;

import io.swagger.annotations.ApiModel;

@ApiModel
@ValidManagementPack
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagementPackRequest extends ManagementPackBase {
}
