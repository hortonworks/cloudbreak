package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.context.VolumeMatcher;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsContextService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsContextService.class);

    @Inject
    private VolumeMatcher volumeMatcher;

    public void addInstancesToContext(List<CloudResource> instances, ResourceBuilderContext context, List<Group> groups) {
        groups.forEach(group -> {
            List<Long> instancePrivateIds = group.getInstances().stream()
                    .filter(instance -> Objects.isNull(instance.getInstanceId()))
                    .map(CloudInstance::getTemplate)
                    .map(InstanceTemplate::getPrivateId)
                    .collect(Collectors.toList());
            LOGGER.info("Instances without instance id: {}", instancePrivateIds);
            List<CloudResource> groupInstances = instances.stream().filter(inst -> inst.getGroup().equals(group.getName())).collect(Collectors.toList());
            if (instancePrivateIds.size() > groupInstances.size()) {
                String message = String.format("Not found enough instances in %s group, expected %s, got %s. " +
                                "Please check the instances on your cloud provider for further details.", group.getName(), instancePrivateIds.size(),
                        groupInstances.size());
                throw new CloudConnectorException(message);
            }
            for (int i = 0; i < instancePrivateIds.size(); i++) {
                context.addComputeResources(instancePrivateIds.get(i), List.of(groupInstances.get(i)));
            }
        });
    }

    public void addResourcesToContext(List<CloudResource> resources, ResourceBuilderContext context, List<Group> groups) {
        groups.forEach(group -> {
            List<CloudInstance> instancesWithoutInstanceId = group.getInstances().stream()
                    .filter(instance -> Objects.isNull(instance.getInstanceId()))
                    .collect(Collectors.toList());
            List<CloudResource> groupInstances = getResourcesOfTypeInGroup(resources, group, ResourceType.AWS_INSTANCE);
            List<CloudResource> groupVolumeSets = getResourcesOfTypeInGroup(resources, group, ResourceType.AWS_VOLUMESET);
            volumeMatcher.addVolumeResourcesToContext(instancesWithoutInstanceId, groupInstances, groupVolumeSets, context);
        });
    }

    private List<CloudResource> getResourcesOfTypeInGroup(List<CloudResource> resources, Group group, ResourceType awsInstance) {
        return resources.stream()
                .filter(cloudResource -> awsInstance.equals(cloudResource.getType()))
                .filter(inst -> inst.getGroup().equals(group.getName())).collect(Collectors.toList());
    }

}
