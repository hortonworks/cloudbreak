package com.sequenceiq.freeipa.api.v1.freeipa.stack.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.FreeIpaServerSettingsModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FreeIpaServerV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FreeIpaServerResponse extends FreeIpaServerBase {

    @Schema(description = FreeIpaServerSettingsModelDescriptions.SERVER_IP, requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> serverIp = new HashSet<>();

    @Schema(description = FreeIpaServerSettingsModelDescriptions.FREEIPA_HOST)
    private String freeIpaHost;

    @Schema(description = FreeIpaServerSettingsModelDescriptions.FREEIPA_PORT)
    private Integer freeIpaPort;

    public Set<String> getServerIp() {
        return serverIp;
    }

    public void setServerIp(Set<String> serverIp) {
        this.serverIp = serverIp;
    }

    public String getFreeIpaHost() {
        return freeIpaHost;
    }

    public void setFreeIpaHost(String freeIpaHost) {
        this.freeIpaHost = freeIpaHost;
    }

    public Integer getFreeIpaPort() {
        return freeIpaPort;
    }

    public void setFreeIpaPort(Integer freeIpaPort) {
        this.freeIpaPort = freeIpaPort;
    }

    @Override
    public String toString() {
        return "FreeIpaServerResponse{" +
                "FreeIpaServerBase=" + super.toString() +
                ", serverIp=" + serverIp +
                ", freeIpaHost='" + freeIpaHost + '\'' +
                ", freeIpaPort=" + freeIpaPort +
                '}';
    }
}
