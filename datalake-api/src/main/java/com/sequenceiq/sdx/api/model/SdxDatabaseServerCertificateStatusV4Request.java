package com.sequenceiq.sdx.api.model;

import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.VM_DATALAKE;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SdxDatabaseServerCertificateStatusV4Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxDatabaseServerCertificateStatusV4Request {

    @NotNull
    @Schema(description = "Crns of the datalakes")
    @ValidCrn(resource = {VM_DATALAKE})
    private Set<String> crns = new HashSet<>();

    public Set<String> getCrns() {
        return crns;
    }

    public void setCrns(Set<String> crns) {
        this.crns = crns;
    }

    @Override
    public String toString() {
        return "SdxDatabaseServerCertificateStatusV4Request{" +
                "crns=" + crns +
                '}';
    }
}
