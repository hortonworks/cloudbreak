package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.imdupdate;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "InstanceMetadataUpdateRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceMetadataUpdateRequest {

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotEmpty
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @NotNull
    @Schema(description = ModelDescriptions.IMD_UPDATE_TYPE, required = true)
    private InstanceMetadataUpdateType updateType;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public InstanceMetadataUpdateType getUpdateType() {
        return updateType;
    }

    public void setUpdateType(InstanceMetadataUpdateType updateType) {
        this.updateType = updateType;
    }

    @Override
    public String toString() {
        return "InstanceMetadataUpdateRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", updateType=" + updateType +
                '}';
    }
}
