package com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ProviderTemplateUpdateHandlerRequest extends StackEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final CloudStack cloudStack;

    private final String volumeType;

    private final int size;

    private final String group;

    private final String diskType;

    @JsonCreator
    public ProviderTemplateUpdateHandlerRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudStack") CloudStack cloudStack,
            @JsonProperty("volumeType") String volumeType,
            @JsonProperty("group") String group,
            @JsonProperty("size") int size,
            @JsonProperty("diskType") String diskType) {
        super(selector, stackId);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.cloudStack = cloudStack;
        this.volumeType = volumeType;
        this.diskType = diskType;
        this.size = size;
        this.group = group;
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

    public String getVolumeType() {
        return volumeType;
    }

    public int getSize() {
        return size;
    }

    public String getGroup() {
        return group;
    }

    public String getDiskType() {
        return diskType;
    }

    public String toString() {
        return new StringJoiner(", ", ProviderTemplateUpdateHandlerRequest.class.getSimpleName() + "[", "]")
                .add("selector=" + super.getSelector())
                .add("stackId=" + super.getResourceId())
                .add("volumeType=" + this.volumeType)
                .add("group=" + this.group)
                .add("size=" + this.size)
                .add("diskType=" + this.diskType)
                .toString();
    }
}
