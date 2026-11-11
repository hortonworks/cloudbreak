package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database;

import jakarta.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.DatabaseServerModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for increasing the storage size of the database server of a cluster")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseServerIncreaseStorageSizeV4Request {

    @Min(1)
    @Schema(description = DatabaseServerModelDescription.STORAGE_SIZE_IN_GB, requiredMode = Schema.RequiredMode.REQUIRED)
    private int storageSizeInGb;

    public int getStorageSizeInGb() {
        return storageSizeInGb;
    }

    public void setStorageSizeInGb(int storageSizeInGb) {
        this.storageSizeInGb = storageSizeInGb;
    }

    @Override
    public String toString() {
        return "DatabaseServerIncreaseStorageSizeV4Request{" +
                "storageSizeInGb=" + storageSizeInGb +
                '}';
    }
}
