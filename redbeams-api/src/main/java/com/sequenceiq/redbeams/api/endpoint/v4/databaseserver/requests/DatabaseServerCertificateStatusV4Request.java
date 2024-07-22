package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.DATABASE_SERVER_CERTIFICATE_REQUEST)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseServerCertificateStatusV4Request {

    @NotNull
    private Set<String> environmentCrns = new HashSet<>();

    public Set<String> getEnvironmentCrns() {
        return environmentCrns;
    }

    public void setEnvironmentCrns(Set<String> environmentCrns) {
        this.environmentCrns = environmentCrns;
    }
}
