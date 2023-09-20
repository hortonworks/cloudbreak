package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_HANDLER_EVENT;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

public class DeleteVolumesHandlerRequest extends StackEvent {

    private final String cloudPlatform;

    private final List<CloudResource> resourcesToBeDeleted;

    private final StackDeleteVolumesRequest stackDeleteVolumesRequest;

    private final Set<ServiceComponent> hostTemplateServiceComponents;

    @JsonCreator
    public DeleteVolumesHandlerRequest(
        @JsonProperty("resourcesToBeDeleted") List<CloudResource> resourcesToBeDeleted,
        @JsonProperty("stackDeleteVolumesRequest") StackDeleteVolumesRequest stackDeleteVolumesRequest,
        @JsonProperty("cloudPlatform") String cloudPlatform,
        @JsonProperty("hostTemplateServiceComponents") Set<ServiceComponent> hostTemplateServiceComponents) {
        super(DELETE_VOLUMES_HANDLER_EVENT.event(), stackDeleteVolumesRequest.getStackId());
        this.resourcesToBeDeleted = resourcesToBeDeleted;
        this.stackDeleteVolumesRequest = stackDeleteVolumesRequest;
        this.cloudPlatform = cloudPlatform;
        this.hostTemplateServiceComponents = hostTemplateServiceComponents;
    }

    public List<CloudResource> getResourcesToBeDeleted() {
        return resourcesToBeDeleted;
    }

    public StackDeleteVolumesRequest getStackDeleteVolumesRequest() {
        return stackDeleteVolumesRequest;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public Set<ServiceComponent> getHostTemplateServiceComponents() {
        return hostTemplateServiceComponents;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DeleteVolumesHandlerRequest.class.getSimpleName() + "[", "]")
                .add("resourcesToBeDeleted=" + resourcesToBeDeleted)
                .add("cloudPlatform=" + cloudPlatform)
                .add("hostTemplateServiceComponents=" + hostTemplateServiceComponents)
                .toString();
    }

    @Override
    public Long getResourceId() {
        return stackDeleteVolumesRequest.getStackId();
    }

    @Override
    public String selector() {
        return getClass().getSimpleName().toUpperCase(Locale.ROOT);
    }
}
