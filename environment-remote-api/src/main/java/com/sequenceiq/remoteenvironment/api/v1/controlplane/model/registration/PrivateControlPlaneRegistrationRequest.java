package com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration;

import java.util.Objects;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PrivateControlPlaneRegistrationV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrivateControlPlaneRegistrationRequest {

    @NotEmpty
    @Schema
    @ValidCrn(resource = CrnResourceDescriptor.HYBRID)
    private String crn;

    @NotEmpty
    @Schema
    private String url;

    @NotEmpty
    @Schema
    private String name;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PrivateControlPlaneRegistrationRequest that = (PrivateControlPlaneRegistrationRequest) o;
        return Objects.equals(crn, that.crn) && Objects.equals(url, that.url) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(crn, url, name);
    }

    @Override
    public String toString() {
        return "PrivateControlPlaneRegistrationRequest{" +
                "crn='" + crn + '\'' +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
