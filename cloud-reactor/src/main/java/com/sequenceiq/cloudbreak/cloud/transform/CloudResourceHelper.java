package com.sequenceiq.cloudbreak.cloud.transform;

import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class CloudResourceHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourceHelper.class);

    @Inject
    private ResourceNotifier resourceNotifier;

    public static void validateRequestCloudResource(CloudResource cloudResource, ResourceType resourceTypeExpected) {
        throwIfNull(cloudResource, () -> new IllegalArgumentException("request.CloudResource must not be null!"));
        throwIfNull(cloudResource.getReference(), () -> new IllegalArgumentException("request.CloudResource.reference must not be null!"));
        ResourceType resourceType = cloudResource.getType();
        if (!resourceTypeExpected.equals(resourceType)) {
            throw new IllegalArgumentException(String.format("request.CloudResource has the wrong resource type! Expected: %s, actual: %s",
                    resourceTypeExpected, resourceType));
        }
    }

    public List<Group> getScaledGroups(CloudStack stack) {
        return stack.getGroups().stream().filter(g -> g.getInstances().stream().anyMatch(
                inst -> InstanceStatus.CREATE_REQUESTED == inst.getTemplate().getStatus())).collect(Collectors.toList());
    }

    public Optional<CloudResource> getResourceTypeFromList(ResourceType type, List<CloudResource> resources) {
        return getResourceTypeInstancesFromList(type, resources)
                .stream()
                .findFirst();
    }

    public List<CloudResource> getResourceTypeInstancesFromList(ResourceType type, List<CloudResource> resources) {
        return resources.stream()
                .filter(resource -> resource.getType() == type)
                .toList();
    }

    public void updateDeleteOnTerminationFlag(List<CloudResource> reattachableVolumeSets, boolean deleteOnTermination, CloudContext cloudContext) {
        if (!reattachableVolumeSets.isEmpty()) {
            List<CloudResource> updatableVolumeSets = new ArrayList<>();
            reattachableVolumeSets.forEach(cloudResource -> {
                if (cloudResource.hasParameter(CloudResource.ATTRIBUTES)) {
                    try {
                        VolumeSetAttributes volumeSetAttributes = cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
                        volumeSetAttributes.setDeleteOnTermination(deleteOnTermination);
                        cloudResource.putParameter(CloudResource.ATTRIBUTES, volumeSetAttributes);
                        updatableVolumeSets.add(cloudResource);
                    } catch (RuntimeException e) {
                        LOGGER.warn("deleteOnTermination flag can not be updated", e);
                    }
                }
            });
            if (!updatableVolumeSets.isEmpty()) {
                resourceNotifier.notifyUpdates(updatableVolumeSets, cloudContext);
            }
        }
    }

}
