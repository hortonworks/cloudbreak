package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template;

import java.io.Serializable;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsInstanceTemplateV1Parameters implements Serializable {

    @Valid
    @Schema(description = TemplateModelDescription.AWS_SPOT_PARAMETERS)
    private AwsInstanceTemplateV1SpotParameters spot;

    @Schema(description = TemplateModelDescription.ENCRYPTION)
    private AwsEncryptionV1Parameters encryption;

    @Valid
    @Schema(description = TemplateModelDescription.AWS_PLACEMENT_GROUP)
    private AwsPlacementGroupV1Parameters placementGroup;

    public AwsInstanceTemplateV1SpotParameters getSpot() {
        return spot;
    }

    public void setSpot(AwsInstanceTemplateV1SpotParameters spot) {
        this.spot = spot;
    }

    public AwsEncryptionV1Parameters getEncryption() {
        return encryption;
    }

    public void setEncryption(AwsEncryptionV1Parameters encryption) {
        this.encryption = encryption;
    }

    public AwsPlacementGroupV1Parameters getPlacementGroup() {
        return placementGroup;
    }

    public void setPlacementGroup(AwsPlacementGroupV1Parameters placementGroup) {
        this.placementGroup = placementGroup;
    }
}
