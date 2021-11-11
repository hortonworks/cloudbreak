package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;

@Component
public class StackScaleV4RequestToUpdateStackV4RequestConverter {

    @Inject
    private InstanceGroupService instanceGroupService;

    public UpdateStackV4Request convert(StackScaleV4Request source) {
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setWithClusterEvent(true);
        Optional<InstanceGroup> instanceGroup = instanceGroupService.findOneWithInstanceMetadataByGroupNameInStack(source.getStackId(), source.getGroup());
        if (instanceGroup.isPresent()) {
            InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = new InstanceGroupAdjustmentV4Request();
            instanceGroupAdjustmentJson.setInstanceGroup(source.getGroup());
            int scaleNumber = source.getDesiredCount() - instanceGroup.get().getNodeCount();
            instanceGroupAdjustmentJson.setScalingAdjustment(scaleNumber);
            instanceGroupAdjustmentJson.setNetworkScaleRequest(source.getStackNetworkScaleV4Request());
            instanceGroupAdjustmentJson.setAdjustmentType(source.getAdjustmentType());
            instanceGroupAdjustmentJson.setThreshold(source.getThreshold());
            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
        } else {
            throw new BadRequestException(String.format("Group '%s' not available on stack", source.getGroup()));
        }
        return updateStackJson;
    }
}
