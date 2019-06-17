package com.sequenceiq.environment.api.v1.environment.model.response;

import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(subTypes = {DetailedEnvironmentResponse.class, SimpleEnvironmentResponse.class})
public abstract class EnvironmentBaseResponse {
    @ApiModelProperty(ModelDescriptions.ID)
    private String crn;

    @ApiModelProperty(ModelDescriptions.NAME)
    private String name;

    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(EnvironmentModelDescription.CLOUD_PLATFORM)
    private String cloudPlatform;

    @ApiModelProperty(ModelDescriptions.CREATOR)
    private String creator;

    @ApiModelProperty(EnvironmentModelDescription.CREATE_FREEIPA)
    private Boolean createFreeIpa = Boolean.TRUE;

    @ApiModelProperty(EnvironmentModelDescription.CREDENTIAL_RESPONSE)
    private CredentialResponse credential;

    @ApiModelProperty(EnvironmentModelDescription.REGIONS)
    private CompactRegionResponse regions;

    @ApiModelProperty(EnvironmentModelDescription.LOCATION)
    private LocationResponse location;

    @ApiModelProperty(EnvironmentModelDescription.NETWORK)
    private EnvironmentNetworkResponse network;

    @ApiModelProperty(EnvironmentModelDescription.STATUS)
    private EnvironmentStatus environmentStatus;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CompactRegionResponse getRegions() {
        return regions;
    }

    public void setRegions(CompactRegionResponse regions) {
        this.regions = regions;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public CredentialResponse getCredential() {
        return credential;
    }

    public void setCredential(CredentialResponse credential) {
        this.credential = credential;
    }

    public LocationResponse getLocation() {
        return location;
    }

    public void setLocation(LocationResponse location) {
        this.location = location;
    }

    public EnvironmentNetworkResponse getNetwork() {
        return network;
    }

    public void setNetwork(EnvironmentNetworkResponse network) {
        this.network = network;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }

    public void setEnvironmentStatus(EnvironmentStatus environmentStatus) {
        this.environmentStatus = environmentStatus;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Boolean getCreateFreeIpa() {
        return createFreeIpa;
    }

    public void setCreateFreeIpa(Boolean createFreeIpa) {
        this.createFreeIpa = createFreeIpa;
    }
}
