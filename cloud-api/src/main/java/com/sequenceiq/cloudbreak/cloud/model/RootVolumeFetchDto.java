package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

public class RootVolumeFetchDto {

    private AuthenticatedContext authenticatedContext;

    private Group group;

    private String azureResourceGroupName;

    private List<CloudResource> cloudResourceList;

    @JsonCreator
    public RootVolumeFetchDto(
            @JsonProperty("authenticatedContext") AuthenticatedContext authenticatedContext,
            @JsonProperty("group") Group group,
            @JsonProperty("resourceGroupName") String azureResourceGroupName,
            @JsonProperty("cloudResourceList") List<CloudResource> cloudResourceList) {
        this.authenticatedContext = authenticatedContext;
        this.group = group;
        this.azureResourceGroupName = azureResourceGroupName;
        this.cloudResourceList = ImmutableList.copyOf(cloudResourceList);
    }

    public AuthenticatedContext getAuthenticatedContext() {
        return authenticatedContext;
    }

    public void setAuthenticatedContext(AuthenticatedContext authenticatedContext) {
        this.authenticatedContext = authenticatedContext;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public String getAzureResourceGroupName() {
        return azureResourceGroupName;
    }

    public void setAzureResourceGroupName(String azureResourceGroupName) {
        this.azureResourceGroupName = azureResourceGroupName;
    }

    public List<CloudResource> getCloudResourceList() {
        return cloudResourceList;
    }

    public void setCloudResourceList(List<CloudResource> cloudResourceList) {
        this.cloudResourceList = cloudResourceList;
    }

    @Override
    public String toString() {
        return "RootVolumeFetchDto{" +
                "authenticatedContext=" + authenticatedContext +
                ", group=" + group +
                ", azureResourceGroupName=" + azureResourceGroupName +
                ", cloudResourceList='" + cloudResourceList +
                '}';
    }
}
