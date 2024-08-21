package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request containing information about a database server SSL certificate")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterDatabaseServerCertificateStatusV4Request {

    @NotNull
    @Schema(description = "Crns of the stacks")
    private Set<String> crns = new HashSet<>();

    public @NotNull Set<String> getCrns() {
        return crns;
    }

    public void setCrns(@NotNull Set<String> crns) {
        this.crns = crns;
    }
}
