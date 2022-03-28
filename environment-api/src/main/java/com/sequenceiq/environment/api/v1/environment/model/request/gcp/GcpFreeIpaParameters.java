package com.sequenceiq.environment.api.v1.environment.model.request.gcp;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "AttachedFreeIpaRequestGcpParameters")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GcpFreeIpaParameters implements Serializable {

    @Override
    public String toString() {
        return "GcpFreeIpaParameters{}";
    }
}
