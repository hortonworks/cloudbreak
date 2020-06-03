package com.sequenceiq.cloudbreak.cloud.model.prerequisite;

import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class EnvironmentPrerequisiteDeleteRequest {

    private final CloudCredential cloudCredential;

    private Optional<AzurePrerequisiteDeleteRequest> azurePrerequisiteDeleteRequest = Optional.empty();

    public EnvironmentPrerequisiteDeleteRequest(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public Optional<AzurePrerequisiteDeleteRequest> getAzurePrerequisiteDeleteRequest() {
        return azurePrerequisiteDeleteRequest;
    }

    public EnvironmentPrerequisiteDeleteRequest withAzurePrerequisiteDeleteRequest(AzurePrerequisiteDeleteRequest azurePrerequisiteDeleteRequest) {
        this.azurePrerequisiteDeleteRequest = Optional.of(azurePrerequisiteDeleteRequest);
        return this;
    }
}
