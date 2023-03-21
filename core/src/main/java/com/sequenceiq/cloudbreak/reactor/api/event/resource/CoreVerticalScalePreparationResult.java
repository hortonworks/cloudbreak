package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.domain.stack.cluster.InstanceStorageInfo;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

public class CoreVerticalScalePreparationResult extends CloudPlatformResult implements FlowPayload {

    private final List<InstanceStorageInfo> instanceStorageInfo;

    private final StackVerticalScaleV4Request stackVerticalScaleV4Request;

    private final Set<ServiceComponent> groupServiceComponents;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final CloudStack cloudStack;

    private final List<String> hostTemplateRoleGroupNames;

    @JsonCreator
    public CoreVerticalScalePreparationResult(
            @JsonProperty("groupServiceComponents") Set<ServiceComponent> groupServiceComponents,
            @JsonProperty("instanceStorageInfo") List<InstanceStorageInfo> instanceStorageInfo,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudStack") CloudStack stack,
            @JsonProperty("stackVerticalScaleV4Request") StackVerticalScaleV4Request stackVerticalScaleV4Request,
            @JsonProperty("hostTemplateRoleGroupNames") List<String> hostTemplateRoleGroupNames) {
        super(stackVerticalScaleV4Request.getStackId());
        this.groupServiceComponents = groupServiceComponents;
        this.instanceStorageInfo = instanceStorageInfo;
        this.stackVerticalScaleV4Request = stackVerticalScaleV4Request;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.cloudStack = stack;
        this.hostTemplateRoleGroupNames = hostTemplateRoleGroupNames;
    }

    public CoreVerticalScalePreparationResult(String statusReason, Exception ex, StackVerticalScaleV4Request stackVerticalScaleV4Request) {
        super(statusReason, ex, stackVerticalScaleV4Request.getStackId());
        this.stackVerticalScaleV4Request = stackVerticalScaleV4Request;
        groupServiceComponents = new HashSet<>();
        instanceStorageInfo = new ArrayList<>();
        hostTemplateRoleGroupNames = new ArrayList<>();
        cloudContext = null;
        cloudCredential = null;
        cloudStack = null;
    }

    public StackVerticalScaleV4Request getStackVerticalScaleV4Request() {
        return stackVerticalScaleV4Request;
    }

    public List<InstanceStorageInfo> getInstanceStorageInfo() {
        return instanceStorageInfo;
    }

    public Set<ServiceComponent> getGroupServiceComponents() {
        return groupServiceComponents;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    public List<String> getHostTemplateRoleGroupNames() {
        return hostTemplateRoleGroupNames;
    }

    public String toString() {
        return new StringJoiner(", ", CoreVerticalScalePreparationResult.class.getSimpleName() + '[', "]")
                .add("instanceStorageInfo=" + instanceStorageInfo)
                .add("groupServiceComponents=" + groupServiceComponents)
                .add("stackVerticalScaleV4Request=" + stackVerticalScaleV4Request)
                .add("cloudStack=" + cloudStack)
                .add("cloudCredential=" + cloudCredential)
                .add("cloudContext=" + cloudContext)
                .add("hostTemplateRoleGroupNames=" + hostTemplateRoleGroupNames)
                .toString();
    }
}
