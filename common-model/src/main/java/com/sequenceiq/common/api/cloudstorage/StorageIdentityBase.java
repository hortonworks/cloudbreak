package com.sequenceiq.common.api.cloudstorage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.model.CloudIdentityType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StorageIdentityBase extends CloudStorageV1Base {

    private CloudIdentityType type;

    public CloudIdentityType getType() {
        return type;
    }

    public void setType(CloudIdentityType type) {
        this.type = type;
    }
}
