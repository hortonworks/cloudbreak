package com.sequenceiq.consumption.api.v1.consumption.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "StorageConsumptionScheduleRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class StorageConsumptionScheduleRequest extends StorageConsumptionBaseRequest {

    @Override
    public String toString() {
        return super.toString() + ", StorageConsumptionScheduleRequest{}";
    }
}
