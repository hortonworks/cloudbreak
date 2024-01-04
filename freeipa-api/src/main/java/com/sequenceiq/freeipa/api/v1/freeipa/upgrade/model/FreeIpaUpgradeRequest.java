package com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FreeIpaUpgradeV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeIpaUpgradeRequest {

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotEmpty
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    private ImageSettingsRequest image;

    private Boolean allowMajorOsUpgrade;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public ImageSettingsRequest getImage() {
        return image;
    }

    public void setImage(ImageSettingsRequest image) {
        this.image = image;
    }

    public Boolean getAllowMajorOsUpgrade() {
        return allowMajorOsUpgrade;
    }

    public void setAllowMajorOsUpgrade(Boolean allowMajorOsUpgrade) {
        this.allowMajorOsUpgrade = allowMajorOsUpgrade;
    }

    @Override
    public String toString() {
        return "FreeIpaUpgradeRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", image=" + image +
                ", allowMajorOsUpgrade=" + allowMajorOsUpgrade +
                '}';
    }
}
