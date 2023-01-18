package com.sequenceiq.consumption.api.v1.consumption.model.request;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.consumption.api.doc.ConsumptionModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class StorageConsumptionRequest extends ConsumptionBaseRequest {

    @NotEmpty
    @Schema(description = ConsumptionModelDescription.STORAGE_LOCATION, required = true)
    private String storageLocation;

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", StorageConsumptionRequest{" +
                "storageLocation='" + storageLocation + '\'' +
                '}';
    }

}
