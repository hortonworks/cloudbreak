package com.sequenceiq.sdx.api.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.common.api.tag.request.TaggableRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxClusterRequestBase implements TaggableRequest {

    @NotNull
    @Schema(description = ModelDescriptions.ENVIRONMENT_NAME)
    private String environment;

    @NotNull
    @Schema(description = ModelDescriptions.CLUSTER_SHAPE)
    private SdxClusterShape clusterShape;

    @Schema(description = ModelDescriptions.CLOUD_STORAGE_DETAILS)
    private SdxCloudStorageRequest cloudStorage;

    @Schema(description = ModelDescriptions.EXTERNAL_DATABASE_OPTIONS)
    private SdxDatabaseRequest externalDatabase;

    @Valid
    @Schema(description = ModelDescriptions.AWS_OPTIONS)
    private SdxAwsRequest aws;

    @Schema(description = ModelDescriptions.AZURE_OPTIONS)
    private SdxAzureRequest azure;

    @Schema(description = ModelDescriptions.TAGS)
    private Map<String, String> tags;

    @Schema(description = ModelDescriptions.RANGER_RAZ_ENABLED)
    private boolean enableRangerRaz;

    @Schema(description = ModelDescriptions.RANGER_RMS_ENABLED)
    private boolean enableRangerRms;

    @Schema(description = ModelDescriptions.MULTI_AZ_ENABLED)
    private boolean enableMultiAz;

    @Valid
    @Schema(description = ModelDescriptions.CUSTOM_INSTANCE_GROUP_OPTIONS)
    private List<SdxInstanceGroupRequest> customInstanceGroups;

    @Schema(description = ModelDescriptions.JAVA_VERSION)
    private Integer javaVersion;

    @Schema(description = ModelDescriptions.RECIPES)
    private Set<SdxRecipe> recipes;

    @Schema(description = ModelDescriptions.IMAGE_SETTINGS)
    private ImageSettingsV4Request image;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public SdxClusterShape getClusterShape() {
        return clusterShape;
    }

    public void setClusterShape(SdxClusterShape clusterShape) {
        this.clusterShape = clusterShape;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    @Override
    public void addTag(String key, String value) {
        initAndGetTags().put(key, value);
    }

    public Map<String, String> initAndGetTags() {
        if (tags == null) {
            tags = new HashMap<>();
        }
        return tags;
    }

    public void addTags(Map<String, String> tags) {
        initAndGetTags().putAll(tags);
    }

    public SdxCloudStorageRequest getCloudStorage() {
        return cloudStorage;
    }

    public void setCloudStorage(SdxCloudStorageRequest cloudStorage) {
        this.cloudStorage = cloudStorage;
    }

    public SdxDatabaseRequest getExternalDatabase() {
        return externalDatabase;
    }

    public void setExternalDatabase(SdxDatabaseRequest externalDatabase) {
        this.externalDatabase = externalDatabase;
    }

    public SdxAwsRequest getAws() {
        return aws;
    }

    public void setAws(SdxAwsRequest aws) {
        this.aws = aws;
    }

    public boolean isEnableRangerRaz() {
        return enableRangerRaz;
    }

    public void setEnableRangerRaz(boolean enableRangerRaz) {
        this.enableRangerRaz = enableRangerRaz;
    }

    public boolean isEnableMultiAz() {
        return enableMultiAz;
    }

    public void setEnableMultiAz(boolean enableMultiAz) {
        this.enableMultiAz = enableMultiAz;
    }

    public SdxAzureRequest getAzure() {
        return azure;
    }

    public void setAzure(SdxAzureRequest azure) {
        this.azure = azure;
    }

    public List<SdxInstanceGroupRequest> getCustomInstanceGroups() {
        return customInstanceGroups;
    }

    public void setCustomInstanceGroups(List<SdxInstanceGroupRequest> customInstanceGroups) {
        this.customInstanceGroups = customInstanceGroups;
    }

    public Integer getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(Integer javaVersion) {
        this.javaVersion = javaVersion;
    }

    public Set<SdxRecipe> getRecipes() {
        return recipes;
    }

    public void setRecipes(Set<SdxRecipe> recipes) {
        this.recipes = recipes;
    }

    public ImageSettingsV4Request getImage() {
        return image;
    }

    public void setImage(ImageSettingsV4Request image) {
        this.image = image;
    }

    public boolean isEnableRangerRms() {
        return enableRangerRms;
    }

    public void setEnableRangerRms(boolean enableRangerRms) {
        this.enableRangerRms = enableRangerRms;
    }

    public void copyTo(SdxClusterRequestBase toInstance) {
        toInstance.setEnvironment(environment);
        toInstance.setClusterShape(clusterShape);
        toInstance.setTags(tags);
        toInstance.setCloudStorage(cloudStorage);
        toInstance.setExternalDatabase(externalDatabase);
        toInstance.setAws(aws);
        toInstance.setEnableRangerRaz(enableRangerRaz);
        toInstance.setEnableMultiAz(enableMultiAz);
        toInstance.setCustomInstanceGroups(customInstanceGroups);
        toInstance.setJavaVersion(javaVersion);
        toInstance.setRecipes(recipes);
        toInstance.setImage(image);
    }

    @Override
    public String toString() {
        return "SdxClusterRequestBase{" +
                "clusterShape=" + clusterShape +
                ", cloudStorage=" + cloudStorage +
                ", externalDatabase=" + externalDatabase +
                ", aws=" + aws +
                ", enableRangerRaz=" + enableRangerRaz +
                ", enabledRangerRms=" + enableRangerRms +
                ", enableMultiAz=" + enableMultiAz +
                ", javaVersion=" + javaVersion +
                ", recipes=" + recipes +
                ", image=" + image +
                '}';
    }
}
