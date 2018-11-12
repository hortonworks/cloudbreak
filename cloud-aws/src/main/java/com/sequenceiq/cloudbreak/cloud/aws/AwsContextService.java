package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class AwsContextService {

    public void addInstancesToContext(List<CloudResource> instances, ResourceBuilderContext context, List<Group> groups) {
        groups.forEach(group -> {
            List<Long> ids = group.getInstances().stream()
                    .filter(instance -> Objects.isNull(instance.getInstanceId()))
                    .map(CloudInstance::getTemplate)
                    .map(InstanceTemplate::getPrivateId)
                    .collect(Collectors.toList());
            List<CloudResource> groupInstances = instances.stream().filter(inst -> inst.getGroup().equals(group.getName())).collect(Collectors.toList());
            for (int i = 0; i < ids.size(); i++) {
                context.addComputeResources(ids.get(i), List.of(groupInstances.get(i)));
            }
        });
    }

    public void addResourcesToContext(List<CloudResource> resources, ResourceBuilderContext context, List<Group> groups) {
        groups.forEach(group -> {
            List<Long> ids = group.getInstances().stream()
                    .filter(instance -> Objects.isNull(instance.getInstanceId()))
                    .map(CloudInstance::getTemplate).map(InstanceTemplate::getPrivateId).collect(Collectors.toList());
            List<CloudResource> groupInstances = getResourcesOfTypeInGroup(resources, group, ResourceType.AWS_INSTANCE);
            List<CloudResource> groupVolumeSets = getResourcesOfTypeInGroup(resources, group, ResourceType.AWS_VOLUMESET);
            for (int i = 0; i < ids.size(); i++) {
                context.addComputeResources(ids.get(i), List.of(groupInstances.get(i), groupVolumeSets.get(i)));
            }
        });
    }

    private List<CloudResource> getResourcesOfTypeInGroup(List<CloudResource> resources, Group group, ResourceType awsInstance) {
        return resources.stream()
                .filter(cloudResource -> awsInstance.equals(cloudResource.getType()))
                .filter(inst -> inst.getGroup().equals(group.getName())).collect(Collectors.toList());
    }

}
