package com.sequenceiq.distrox.api.v1.distrox.model.security;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BaseSecurityV1")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseSecurityV1 {

    @Schema(description = "SELinux enabled on the image.")
    private String seLinux;

    public String getSeLinux() {
        return seLinux;
    }

    public void setSeLinux(String seLinux) {
        this.seLinux = seLinux;
    }

    @Override
    public String toString() {
        return "DistroXV1Request.BaseSecurityV1{" +
                "seLinux='" + seLinux + '\'' +
                '}';
    }
}
