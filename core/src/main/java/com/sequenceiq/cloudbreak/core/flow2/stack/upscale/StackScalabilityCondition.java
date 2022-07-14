package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
class StackScalabilityCondition {

    boolean isScalable(StackDtoDelegate stack, String instanceGroupName) {
        InstanceGroupDto instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        List<InstanceStatus> statuses = getStatuses(instanceGroup);
        return statuses.stream().noneMatch(status -> status.equals(InstanceStatus.REQUESTED));

    }

    private List<InstanceStatus> getStatuses(InstanceGroupDto instanceGroup) {
        return instanceGroup.getInstanceMetadataViews().stream()
                .map(InstanceMetadataView::getInstanceStatus)
                .collect(Collectors.toList());
    }
}
