package com.sequenceiq.environment.api.v1.environment.model.request;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentDatabaseServerCertificateStatusV4Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentDatabaseServerCertificateStatusV4Request {

    @NotNull
    @NotEmpty
    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    private Set<String> environmentCrns = new HashSet<>();

    public Set<String> getEnvironmentCrns() {
        return environmentCrns;
    }

    public void setEnvironmentCrns(Set<String> environmentCrns) {
        this.environmentCrns = environmentCrns;
    }

    @Override
    public String toString() {
        return "EnvironmentDatabaseServerCertificateStatusV4Request{" +
                "environmentCrns=" + environmentCrns +
                '}';
    }
}
