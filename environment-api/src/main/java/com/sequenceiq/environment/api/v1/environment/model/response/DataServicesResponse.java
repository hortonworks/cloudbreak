package com.sequenceiq.environment.api.v1.environment.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.environment.model.base.DataServicesBase;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataServicesResponse extends DataServicesBase {

    @Override
    public String toString() {
        return "DataServicesResponse{} " + super.toString();
    }
}
