package com.sequenceiq.cloudbreak.api.model.rds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel("oracle")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OracleParameters extends RdsConfigRequestParameters {

}
