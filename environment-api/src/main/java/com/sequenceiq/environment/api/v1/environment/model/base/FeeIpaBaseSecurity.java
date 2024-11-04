package com.sequenceiq.environment.api.v1.environment.model.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FeeIpaBaseSecurity")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class FeeIpaBaseSecurity {

    @Schema(description = EnvironmentModelDescription.SELINUX)
    private String seLinux;

    public String getSeLinux() {
        return seLinux;
    }

    public void setSeLinux(String seLinux) {
        this.seLinux = seLinux;
    }

    @Override
    public String toString() {
        return "FeeIpaBaseSecurity{" +
                "seLinux='" + seLinux.toString() + '\'' +
                '}';
    }
}
