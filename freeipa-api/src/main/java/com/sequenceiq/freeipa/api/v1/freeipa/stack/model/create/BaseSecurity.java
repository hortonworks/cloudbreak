package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BaseSecurity")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseSecurity {

    @Schema(description = FreeIpaModelDescriptions.FreeIpaImageSecurityModelDescriptions.SELINUX)
    private String seLinux;

    public String getSeLinux() {
        return seLinux;
    }

    public void setSeLinux(String seLinux) {
        this.seLinux = seLinux;
    }

    @Override
    public String toString() {
        return "CreateFreeIpaRequest.BaseSecurity{" +
                "seLinux='" + seLinux.toString() + '\'' +
                '}';
    }
}
