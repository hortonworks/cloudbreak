package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BaseSecurityV4")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseSecurityV4 {

    @Schema(description = "SELinux policy enabled on the image.")
    private String seLinux;

    public String getSeLinux() {
        return seLinux;
    }

    public void setSeLinux(String seLinux) {
        this.seLinux = seLinux;
    }

    @Override
    public String toString() {
        return "StackV4Request.BaseSecurityV4{" +
                "seLinux='" + seLinux + '\'' +
                '}';
    }
}
