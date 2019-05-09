package com.sequenceiq.environment.api.environment.model.response;

import java.util.Set;

import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public abstract class EnvironmentV1BaseResponse {
    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(ModelDescriptions.NAME)
    private String name;

    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(EnvironmentModelDescription.REGIONS)
    private CompactRegionV1Response regions;

    @ApiModelProperty(EnvironmentModelDescription.CLOUD_PLATFORM)
    private String cloudPlatform;

    @ApiModelProperty(EnvironmentModelDescription.CREDENTIAL_NAME_RESPONSE)
    private String credentialName;

    @ApiModelProperty(EnvironmentModelDescription.LOCATION)
    private LocationV1Response location;

    @ApiModelProperty(EnvironmentModelDescription.DATALAKE_RESOURCES_NAMES)
    private Set<String> datalakeResourcesNames;

    @ApiModelProperty(EnvironmentModelDescription.DATALAKE_CLUSTER_NAMES)
    private Set<String> datalakeClusterNames;

    @ApiModelProperty(EnvironmentModelDescription.WORKLOAD_CLUSTER_NAMES)
    private Set<String> workloadClusterNames;

    @ApiModelProperty(EnvironmentModelDescription.NETWORK)
    private EnvironmentNetworkV1Response network;

    public Set<String> getDatalakeResourcesNames() {
        return datalakeResourcesNames;
    }

    public void setDatalakeResourcesNames(Set<String> datalakeResourcesNames) {
        this.datalakeResourcesNames = datalakeResourcesNames;
    }

    public Set<String> getDatalakeClusterNames() {
        return datalakeClusterNames;
    }

    public void setDatalakeClusterNames(Set<String> datalakeClusterNames) {
        this.datalakeClusterNames = datalakeClusterNames;
    }

    public Set<String> getWorkloadClusterNames() {
        return workloadClusterNames;
    }

    public void setWorkloadClusterNames(Set<String> workloadClusterNames) {
        this.workloadClusterNames = workloadClusterNames;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public CompactRegionV1Response getRegions() {
        return regions;
    }

    public void setRegions(CompactRegionV1Response regions) {
        this.regions = regions;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    public LocationV1Response getLocation() {
        return location;
    }

    public void setLocation(LocationV1Response location) {
        this.location = location;
    }

    public EnvironmentNetworkV1Response getNetwork() {
        return network;
    }

    public void setNetwork(EnvironmentNetworkV1Response network) {
        this.network = network;
    }
}
