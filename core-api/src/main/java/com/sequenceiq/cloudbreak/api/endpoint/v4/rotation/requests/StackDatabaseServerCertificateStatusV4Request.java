package com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.requests;

import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.DATAHUB;
import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.VM_DATALAKE;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StackDatabaseServerCertificateStatusV4Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackDatabaseServerCertificateStatusV4Request {

    @NotNull
    @Schema(description = "Crns of the datahubs or datalakes")
    @ValidCrn(resource = {DATAHUB, VM_DATALAKE})
    private Set<String> crns = new HashSet<>();

    public Set<String> getCrns() {
        return crns;
    }

    public void setCrns(Set<String> crns) {
        this.crns = crns;
    }

    @Override
    public String toString() {
        return "StackDatabaseServerCertificateStatusV4Request{" +
                "crns=" + crns +
                '}';
    }
}
