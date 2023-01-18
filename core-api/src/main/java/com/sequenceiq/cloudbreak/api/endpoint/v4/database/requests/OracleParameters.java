package com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class OracleParameters extends DatabaseRequestParameters {

}
