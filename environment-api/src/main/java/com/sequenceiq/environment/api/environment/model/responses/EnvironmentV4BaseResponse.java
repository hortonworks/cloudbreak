package com.sequenceiq.environment.api.environment.model.responses;

import java.util.Set;

import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.environment.doc.EnvironmentResponseModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public abstract class EnvironmentV4BaseResponse {
    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(ModelDescriptions.NAME)
    private String name;

    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(EnvironmentResponseModelDescription.REGIONS)
    private CompactRegionV4Response regions;

    @ApiModelProperty(EnvironmentResponseModelDescription.CLOUD_PLATFORM)
    private String cloudPlatform;

    @ApiModelProperty(EnvironmentResponseModelDescription.CREDENTIAL_NAME)
    private String credentialName;

    @ApiModelProperty(EnvironmentResponseModelDescription.LOCATION)
    private LocationV4Response location;

    @ApiModelProperty(EnvironmentResponseModelDescription.DATALAKE_RESOURCES_NAMES)
    private Set<String> datalakeResourcesNames;

    @ApiModelProperty(EnvironmentResponseModelDescription.DATALAKE_CLUSTER_NAMES)
    private Set<String> datalakeClusterNames;

    @ApiModelProperty(EnvironmentResponseModelDescription.WORKLOAD_CLUSTER_NAMES)
    private Set<String> workloadClusterNames;

    @ApiModelProperty(EnvironmentResponseModelDescription.NETWORK)
    private EnvironmentNetworkV4Response network;

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

    public CompactRegionV4Response getRegions() {
        return regions;
    }

    public void setRegions(CompactRegionV4Response regions) {
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

    public LocationV4Response getLocation() {
        return location;
    }

    public void setLocation(LocationV4Response location) {
        this.location = location;
    }

    public EnvironmentNetworkV4Response getNetwork() {
        return network;
    }

    public void setNetwork(EnvironmentNetworkV4Response network) {
        this.network = network;
    }
}
