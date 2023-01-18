package com.sequenceiq.environment.api.v1.environment.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.environment.model.base.DataServicesBase;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataServicesRequest extends DataServicesBase {

    @Override
    public String toString() {
        return super.toString() + ", " + "DataServicesRequest{}";
    }
}
