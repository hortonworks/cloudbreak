package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ImageChangeV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageChangeRequest {

    @NotEmpty
    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @NotNull
    @Schema(required = true)
    private ImageSettingsRequest imageSettings;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public ImageSettingsRequest getImageSettings() {
        return imageSettings;
    }

    public void setImageSettings(ImageSettingsRequest imageSettings) {
        this.imageSettings = imageSettings;
    }

    @Override
    public String toString() {
        return "ImageChangeRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", imageSettings=" + imageSettings +
                '}';
    }
}
