package com.sequenceiq.distrox.api.v1.distrox.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistroXInstanceMetadataUpdateV1Request {

    @ValidCrn(resource = CrnResourceDescriptor.DATAHUB)
    @NotEmpty
    @Schema(description = ModelDescriptions.CRN, required = true)
    private String crn;

    @NotNull
    @Schema(description = ModelDescriptions.InstanceMetadataUpdateRequest.IMD_UPDATE_TYPE, required = true)
    private InstanceMetadataUpdateType updateType;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public InstanceMetadataUpdateType getUpdateType() {
        return updateType;
    }

    public void setUpdateType(InstanceMetadataUpdateType updateType) {
        this.updateType = updateType;
    }

    @Override
    public String toString() {
        return "DistroXInstanceMetadataUpdateV1Request{" +
                "crn='" + crn + '\'' +
                ", updateType=" + updateType +
                '}';
    }
}
