package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class StackScaleV4RequestToUpdateStackV4RequestConverter {

    @Inject
    private StackDtoService stackDtoService;

    public UpdateStackV4Request convert(StackScaleV4Request source) {
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setWithClusterEvent(true);
        List<InstanceGroupDto> instanceGroups = stackDtoService.getInstanceMetadataByInstanceGroup(source.getStackId());
        Optional<InstanceGroupDto> instanceGroup = instanceGroups.stream()
                .filter(ig -> ig.getInstanceGroup().getGroupName().equals(source.getGroup()))
                .findFirst();
        if (instanceGroup.isPresent()) {
            InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = new InstanceGroupAdjustmentV4Request();
            instanceGroupAdjustmentJson.setInstanceGroup(source.getGroup());
            int scaleNumber = source.getDesiredCount() - instanceGroup.get().getInstanceMetadataViews().size();
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
