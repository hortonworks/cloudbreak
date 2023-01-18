package com.sequenceiq.common.api.cloudstorage;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.model.CloudStorageCdpService;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StorageLocationBase implements Serializable {

    private CloudStorageCdpService type;

    private String value;

    public CloudStorageCdpService getType() {
        return type;
    }

    public void setType(CloudStorageCdpService type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}