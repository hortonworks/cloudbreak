package com.sequenceiq.environment.api.v1.environment.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.environment.model.base.DataServicesBase;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataServicesRequest extends DataServicesBase {

    @Override
    public String toString() {
        return super.toString() + ", " + "DataServicesRequest{}";
    }
}
