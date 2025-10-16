package com.sequenceiq.remoteenvironment.api.v1.environment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.remoteenvironment.api.v1.environment.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteEnvironmentBase {

    @Schema(description = ModelDescriptions.CRN)
    private String crn;

    @Schema(description = ModelDescriptions.NAME)
    private String name;

    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN)
    private String environmentCrn;

    @Schema(description = ModelDescriptions.URL)
    private String url;

    @Schema(description = ModelDescriptions.CLOUD_PLATFORM)
    private String cloudPlatform;

    @Schema(description = ModelDescriptions.PRIVATE_CONTROL_PLANE_NAME)
    private String privateControlPlaneName;

    @Schema(description = ModelDescriptions.REGION)
    private String region;

    @Schema(description = ModelDescriptions.STATUS)
    private String status;

    @Schema(description = ModelDescriptions.CREATED)
    private Long created;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getPrivateControlPlaneName() {
        return privateControlPlaneName;
    }

    public void setPrivateControlPlaneName(String privateControlPlaneName) {
        this.privateControlPlaneName = privateControlPlaneName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    @Override
    public String toString() {
        return "RemoteEnvironmentBase{" +
                "crn='" + crn + '\'' +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", privateControlPlaneName='" + privateControlPlaneName + '\'' +
                ", region='" + region + '\'' +
                ", status='" + status + '\'' +
                ", created=" + created +
                '}';
    }
}
