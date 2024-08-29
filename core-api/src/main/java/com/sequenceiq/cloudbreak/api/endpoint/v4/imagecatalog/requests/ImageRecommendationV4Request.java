package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.ARCHITECTURE;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.ENVIRONMENT_CRN;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.IMAGE_SETTINGS;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.REGION;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.model.Architecture;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageRecommendationV4Request {

    @NotNull
    @Schema(description = ModelDescriptions.CLOUD_PLATFORM)
    private String platform;

    @NotNull
    @Schema(description = ModelDescriptions.BlueprintModelDescription.BLUEPRINT_NAME)
    private String blueprintName;

    @NotNull
    @Schema(description = ENVIRONMENT_CRN)
    private String environmentCrn;

    @NotNull
    @Schema(description = REGION)
    private String region;

    @Schema(description = ARCHITECTURE)
    private Architecture architecture;

    @Schema(description = IMAGE_SETTINGS)
    private ImageSettingsV4Request image;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Architecture getArchitecture() {
        return architecture;
    }

    public void setArchitecture(Architecture architecture) {
        this.architecture = architecture;
    }

    public void setImage(ImageSettingsV4Request image) {
        this.image = image;
    }

    public ImageSettingsV4Request getImage() {
        return image;
    }

    @Override
    public String toString() {
        return "ImageRecommendationV4Request{" +
                "platform='" + platform + '\'' +
                ", blueprintName='" + blueprintName + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", region='" + region + '\'' +
                ", architecture='" + architecture + '\'' +
                ", image=" + image +
                '}';
    }
}
