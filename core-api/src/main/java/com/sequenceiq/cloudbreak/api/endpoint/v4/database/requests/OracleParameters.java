package com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.rds.RdsConfigRequestParameters;

import io.swagger.annotations.ApiModel;

@ApiModel("oracle")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OracleParameters extends RdsConfigRequestParameters {

}
