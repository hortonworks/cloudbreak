package com.sequenceiq.environment.api.v1.environment.model.request.azure;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AttachedFreeIpaRequestAzureParameters")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureFreeIpaParameters implements Serializable {

    @Override
    public String toString() {
        return "AzureFreeIpaParameters{}";
    }
}
