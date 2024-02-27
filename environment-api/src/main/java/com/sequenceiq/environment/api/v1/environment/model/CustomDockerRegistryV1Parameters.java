package com.sequenceiq.environment.api.v1.environment.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.environment.api.doc.dataservices.DataServicesModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.base.BaseDataServicesV1Parameters;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class CustomDockerRegistryV1Parameters extends BaseDataServicesV1Parameters {

    @ValidCrn(resource = CrnResourceDescriptor.COMPUTE_DOCKER_CONFIG)
    @Schema(description = DataServicesModelDescription.REGISTRY_CONFIG_CRN)
    private String crn;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    @Override
    public String toString() {
        return "CustomDockerRegistryV1Parameters{" +
                "crn='" + crn + '\'' +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CustomDockerRegistryV1Parameters that)) {
            return false;
        }
        return Objects.equals(crn, that.crn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), crn);
    }
}
