package com.sequenceiq.freeipa.api.v1.freeipa.stack.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FreeIpaServerV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FreeIpaServerRequest extends FreeIpaServerBase {
    @NotNull
    private String adminPassword;

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    @Override
    public String toString() {
        return super.toString() + " FreeIpaServerRequest{"
                + "adminPassword='***\'"
                + '}';
    }
}
