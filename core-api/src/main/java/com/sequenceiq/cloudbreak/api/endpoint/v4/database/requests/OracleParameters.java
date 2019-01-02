package com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class OracleParameters extends DatabaseRequestParameters {

}
