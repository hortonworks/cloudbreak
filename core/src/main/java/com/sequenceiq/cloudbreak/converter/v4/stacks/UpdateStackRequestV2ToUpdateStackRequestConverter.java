package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;

@Component
public class UpdateStackRequestV2ToUpdateStackRequestConverter extends AbstractConversionServiceAwareConverter<StackScaleV4Request, UpdateStackV4Request> {

    @Inject
    private InstanceGroupService instanceGroupService;

    @Override
    public UpdateStackV4Request convert(StackScaleV4Request source) {
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setWithClusterEvent(true);
        Optional<InstanceGroup> instanceGroup = instanceGroupService.findOneByGroupNameInStack(source.getStackId(), source.getGroup());
        if (instanceGroup.isPresent()) {
            InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = new InstanceGroupAdjustmentV4Request();
            instanceGroupAdjustmentJson.setInstanceGroup(source.getGroup());
            int scaleNumber = source.getDesiredCount() - instanceGroup.get().getNodeCount();
            instanceGroupAdjustmentJson.setScalingAdjustment(scaleNumber);
            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
        } else {
            throw new BadRequestException(String.format("Group '%s' not available on stack", source.getGroup()));
        }
        return updateStackJson;
    }
}
