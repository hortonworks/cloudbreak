package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.InstanceStorageInfo;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

public class CoreVerticalScaleRequest<T> extends CloudStackRequest<T> {

    private final StackDto stack;

    private final Set<ServiceComponent> groupServiceComponents;

    private final List<InstanceStorageInfo> instanceStorageInfo;

    private final List<CloudResource> resourceList;

    private final StackVerticalScaleV4Request stackVerticalScaleV4Request;

    private final InstanceGroupDto instanceGroup;

    private final List<String> hostTemplateRoleGroupNames;

    public CoreVerticalScaleRequest(@JsonProperty("stack") StackDto stackDto,
            @JsonProperty("instanceGroup") InstanceGroupDto instanceGroup,
            @JsonProperty("groupServiceComponents") Set<ServiceComponent> groupServiceComponents,
            @JsonProperty("instanceStorageInfo") List<InstanceStorageInfo> instanceStorageInfo,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("stack") CloudStack stack,
            @JsonProperty("resourceList") List<CloudResource> resourceList,
            @JsonProperty("stackVerticalScaleV4Request") StackVerticalScaleV4Request stackVerticalScaleV4Request,
            @JsonProperty("hostTemplateRoleGroupNames") List<String> hostTemplateRoleGroupNames) {
        super(cloudContext, cloudCredential, stack);
        this.stack = stackDto;
        this.groupServiceComponents = groupServiceComponents;
        this.instanceStorageInfo = instanceStorageInfo;
        this.resourceList = resourceList;
        this.stackVerticalScaleV4Request = stackVerticalScaleV4Request;
        this.instanceGroup = instanceGroup;
        this.hostTemplateRoleGroupNames = hostTemplateRoleGroupNames;
    }

    public List<CloudResource> getResourceList() {
        return resourceList;
    }

    public StackVerticalScaleV4Request getStackVerticalScaleV4Request() {
        return stackVerticalScaleV4Request;
    }

    public Set<ServiceComponent> getGroupServiceComponents() {
        return groupServiceComponents;
    }

    public InstanceGroupDto getInstanceGroup() {
        return instanceGroup;
    }

    public StackDto getStack() {
        return stack;
    }

    public List<InstanceStorageInfo> getInstanceStorageInfo() {
        return instanceStorageInfo;
    }

    public List<String> getHostTemplateRoleGroupNames() {
        return hostTemplateRoleGroupNames;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CoreVerticalScaleRequest.class.getSimpleName() + '[', "]")
                .add("stackDto=" + stack)
                .add("resourceList=" + resourceList)
                .add("groupServiceComponents=" + groupServiceComponents)
                .add("instanceStorageInfo=" + instanceStorageInfo)
                .add("instanceGroup=" + instanceGroup)
                .add("hostTemplateRoleGroupNames=" + hostTemplateRoleGroupNames)
                .toString();
    }
}
