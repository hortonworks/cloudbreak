package com.sequenceiq.environment.api.v1.environment.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.environment.model.base.DataServicesBase;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataServicesResponse extends DataServicesBase {

    @Override
    public String toString() {
        return "DataServicesResponse{} " + super.toString();
    }
}
