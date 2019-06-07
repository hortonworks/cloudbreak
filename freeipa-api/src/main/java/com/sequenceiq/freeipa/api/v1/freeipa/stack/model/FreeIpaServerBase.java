package com.sequenceiq.freeipa.api.v1.freeipa.stack.model;

import javax.validation.constraints.NotNull;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.FreeIpaServerSettingsModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

public abstract class FreeIpaServerBase {

    @NotNull
    @ApiModelProperty(FreeIpaServerSettingsModelDescriptions.DOMAIN)
    private String domain;

    @NotNull
    @ApiModelProperty(FreeIpaServerSettingsModelDescriptions.HOSTNAME)
    private String hostname;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
