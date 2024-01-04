package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.PRIVATE_ID;
import static java.util.function.Function.identity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.context.VolumeMatcher;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureContextService {

    @Inject
    private VolumeMatcher volumeMatcher;

    public void addInstancesToContext(List<CloudResource> instances, ResourceBuilderContext context, List<Group> groups) {
        groups.forEach(group -> {
            List<Long> privateIds = group.getInstances().stream()
                    .filter(instance -> Objects.isNull(instance.getInstanceId()))
                    .map(CloudInstance::getTemplate)
                    .map(InstanceTemplate::getPrivateId)
                    .collect(Collectors.toList());
            Map<Long, CloudResource> groupInstances = instances.stream()
                    .filter(instance -> instance.getGroup().equals(group.getName()))
                    .collect(Collectors.toMap(instance -> (Long) instance.getParameters().get(PRIVATE_ID), identity()));
            for (Long privateId : privateIds) {
                context.addComputeResources(privateId, List.of(groupInstances.get(privateId)));
            }
        });
    }

    public void addResourcesToContext(List<CloudResource> resources, ResourceBuilderContext context, List<Group> groups) {
        groups.forEach(group -> {
            List<CloudResource> resourceGroup = getResourcesOfTypeInGroup(resources, group, ResourceType.AZURE_RESOURCE_GROUP);
            context.addNetworkResources(resourceGroup);
            List<CloudInstance> instancesWithoutInstanceId =
                    group.getInstances().stream().filter(instance -> Objects.isNull(instance.getInstanceId())).collect(Collectors.toList());
            List<CloudResource> groupInstances = getResourcesOfTypeInGroup(resources, group, ResourceType.AZURE_INSTANCE);
            List<CloudResource> groupVolumeSets = getResourcesOfTypeInGroup(resources, group, ResourceType.AZURE_VOLUMESET);
            volumeMatcher.addVolumeResourcesToContext(instancesWithoutInstanceId, groupInstances, groupVolumeSets, context);
        });
    }

    private List<CloudResource> getResourcesOfTypeInGroup(List<CloudResource> resources, Group group, ResourceType instance) {
        return resources.stream()
                .filter(cloudResource -> instance.equals(cloudResource.getType()))
                .filter(inst -> inst.getGroup().equals(group.getName())).collect(Collectors.toList());
    }

}
