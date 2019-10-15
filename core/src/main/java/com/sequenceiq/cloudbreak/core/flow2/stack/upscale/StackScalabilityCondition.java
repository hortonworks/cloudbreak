package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Component
class StackScalabilityCondition {

    boolean isScalable(Stack stack, String instanceGroupName) {
        InstanceGroup instanceGroup = getInstanceGroupByName(stack, instanceGroupName);
        List<InstanceStatus> statuses = getStatuses(instanceGroup);
        return statuses.stream().noneMatch(status -> status.equals(InstanceStatus.REQUESTED));

    }

    private List<InstanceStatus> getStatuses(InstanceGroup instanceGroup) {
        return instanceGroup.getInstanceMetaDataSet().stream()
                .map(InstanceMetaData::getInstanceStatus)
                .collect(Collectors.toList());
    }

    private InstanceGroup getInstanceGroupByName(Stack stack, String instanceGroupName) {
        return stack.getInstanceGroups().stream()
                .filter(group -> group.getGroupName().equals(instanceGroupName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No instance group found."));
    }
}
