package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.CANARY_DATABASE_PROPERTIES_RESPONSE)
public class CanaryDatabasePropertiesV4Response {

    @NotNull
    @Schema(description = ModelDescriptions.DatabaseServer.HOST)
    private String host;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String
    toString() {
        return "CanaryDatabasePropertiesV4Response{" +
                "host='" + host + '\'' +
                '}';
    }
}