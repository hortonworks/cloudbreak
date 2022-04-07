package com.sequenceiq.consumption.api.v1.consumption.model.request;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.consumption.api.doc.ConsumptionModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(subTypes = {StorageConsumptionScheduleRequest.class, StorageConsumptionUnscheduleRequest.class})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class StorageConsumptionBaseRequest implements Serializable {

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotNull
    @ApiModelProperty(value = ConsumptionModelDescription.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @NotNull
    @ApiModelProperty(value = ConsumptionModelDescription.STORAGE_LOCATION, required = true)
    private String storageLocation;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    @Override
    public String toString() {
        return "StorageConsumptionBaseRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", storageLocation='" + storageLocation + '\'' +
                '}';
    }
}
