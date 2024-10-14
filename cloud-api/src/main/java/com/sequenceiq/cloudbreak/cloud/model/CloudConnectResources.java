package com.sequenceiq.cloudbreak.cloud.model;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;

public class CloudConnectResources {

    private CloudCredential cloudCredential;

    private CloudContext cloudContext;

    private CloudConnector cloudConnector;

    private AuthenticatedContext authenticatedContext;

    private CloudStack cloudStack;

    @JsonCreator
    public CloudConnectResources(
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudConnector") CloudConnector cloudConnector,
            @JsonProperty("authenticatedContext") AuthenticatedContext authenticatedContext,
            @JsonProperty("cloudStack") CloudStack cloudStack
        ) {
        this.cloudCredential = cloudCredential;
        this.cloudContext = cloudContext;
        this.cloudConnector = cloudConnector;
        this.authenticatedContext = authenticatedContext;
        this.cloudStack = cloudStack;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public void setCloudCredential(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public void setCloudContext(CloudContext cloudContext) {
        this.cloudContext = cloudContext;
    }

    public CloudConnector getCloudConnector() {
        return cloudConnector;
    }

    public void setCloudConnector(CloudConnector cloudConnector) {
        this.cloudConnector = cloudConnector;
    }

    public AuthenticatedContext getAuthenticatedContext() {
        return authenticatedContext;
    }

    public void setAuthenticatedContext(AuthenticatedContext authenticatedContext) {
        this.authenticatedContext = authenticatedContext;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    public void setCloudStack(CloudStack cloudStack) {
        this.cloudStack = cloudStack;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CloudConnectResources.class.getSimpleName() + "[", "]")
            .add("cloudContext=" + cloudContext)
            .add("cloudConnector=" + cloudConnector)
            .add("cloudStack=" + cloudStack)
            .toString();
    }
}
