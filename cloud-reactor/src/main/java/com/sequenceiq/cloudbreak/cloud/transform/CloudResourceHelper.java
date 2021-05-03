package com.sequenceiq.cloudbreak.cloud.transform;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class CloudResourceHelper {

    public List<Group> getScaledGroups(CloudStack stack) {
        return stack.getGroups().stream().filter(g -> g.getInstances().stream().anyMatch(
                inst -> InstanceStatus.CREATE_REQUESTED == inst.getTemplate().getStatus())).collect(Collectors.toList());
    }

    public Optional<CloudResource> getResourceTypeFromList(ResourceType type, List<CloudResource> resources) {
        return resources.stream()
                .filter(resource -> resource.getType() == type)
                .findFirst();
    }

}
