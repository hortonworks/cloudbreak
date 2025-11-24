package com.sequenceiq.cloudbreak.cloud.aws.common.providersync;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContextBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.provider.ProviderResourceSyncer;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsVolumeSetSyncer implements ProviderResourceSyncer<ResourceType> {

    @Inject
    private AwsVolumeResourceBuilder awsVolumeResourceBuilder;

    @Inject
    private AwsContextBuilder contextBuilder;

    @Override
    public List<CloudResourceStatus> sync(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        AwsContext context = contextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        List<CloudResource> volumeSets = resources.stream()
                .filter(resource -> resource.getType() == getResourceType())
                .toList();
        for (CloudResource volumeSet : volumeSets) {
            result.addAll(awsVolumeResourceBuilder.checkResources(context, authenticatedContext, List.of(volumeSet)));
        }
        return result;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.AWS_VOLUMESET;
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_DEFAULT_VARIANT;
    }

    @Override
    public boolean shouldPersist() {
        return false;
    }

}
