package com.sequenceiq.environment.api.v1.environment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.environment.api.v1.environment.model.base.BaseDataServicesV1Parameters;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AwsDataServicesV1Parameters extends BaseDataServicesV1Parameters {

    @Override
    public String toString() {
        return "AwsDataServicesV1Parameters{} " + super.toString();
    }
}
