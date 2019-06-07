package com.sequenceiq.freeipa.api.v1.freeipa.stack.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.FreeIpaServerSettingsModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("FreeIpaServerV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FreeIpaServerResponse extends FreeIpaServerBase {

    @ApiModelProperty(FreeIpaServerSettingsModelDescriptions.SERVER_IP)
    private Set<String> serverIp = new HashSet<>();

    public Set<String> getServerIp() {
        return serverIp;
    }

    public void setServerIp(Set<String> serverIp) {
        this.serverIp = serverIp;
    }
}
