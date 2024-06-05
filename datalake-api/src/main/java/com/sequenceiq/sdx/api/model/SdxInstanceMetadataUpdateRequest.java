package com.sequenceiq.sdx.api.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxInstanceMetadataUpdateRequest {

    @ValidCrn(resource = CrnResourceDescriptor.VM_DATALAKE)
    @NotNull
    @Schema(description = ModelDescriptions.DATA_LAKE_CRN)
    private String crn;

    @NotNull
    @Schema(description = ModelDescriptions.IMD_UPDATE_TYPE)
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
        return "SdxInstanceMetadataUpdateRequest{" +
                "crn='" + crn + '\'' +
                ", updateType=" + updateType +
                '}';
    }
}
