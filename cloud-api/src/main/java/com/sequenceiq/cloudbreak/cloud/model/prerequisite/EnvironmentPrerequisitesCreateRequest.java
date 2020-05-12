package com.sequenceiq.cloudbreak.cloud.model.prerequisite;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class EnvironmentPrerequisitesCreateRequest {

    private final CloudCredential cloudCredential;

    private final AzurePrerequisiteCreateRequest azure;

    public EnvironmentPrerequisitesCreateRequest(CloudCredential cloudCredential, AzurePrerequisiteCreateRequest azure) {
        this.cloudCredential = cloudCredential;
        this.azure = azure;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public AzurePrerequisiteCreateRequest getAzure() {
        return azure;
    }
}
