package com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DbConnectionParamsV4Response {

    private String usernameVaultPath;

    private String passwordVaultPath;

    @JsonCreator
    public DbConnectionParamsV4Response(
            @JsonProperty("usernameVaultPath") String usernameVaultPath,
            @JsonProperty("passwordVaultPath") String passwordVaultPath) {
        this.usernameVaultPath = usernameVaultPath;
        this.passwordVaultPath = passwordVaultPath;
    }

    public String getUsernameVaultPath() {
        return usernameVaultPath;
    }

    public String getPasswordVaultPath() {
        return passwordVaultPath;
    }
}
