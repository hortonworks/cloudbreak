package com.sequenceiq.cloudbreak.converter.v2;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.api.model.UpdateStackRequestV2;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;

@Component
public class UpdateStackRequestV2ToUpdateStackRequestConverter extends AbstractConversionServiceAwareConverter<UpdateStackRequestV2, UpdateStackJson> {

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Override
    public UpdateStackJson convert(UpdateStackRequestV2 source) {
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setStatus(source.getStatus());
        updateStackJson.setWithClusterEvent(source.getWithClusterEvent());
        if (source.getInstanceGroupAdjustment() != null) {
            InstanceGroup instanceGroup =
                    instanceGroupRepository.findOneByGroupNameInStack(source.getStackId(), source.getInstanceGroupAdjustment().getInstanceGroup());
            if (instanceGroup != null) {
                InstanceGroupAdjustmentJson instanceGroupAdjustmentJson = new InstanceGroupAdjustmentJson();
                instanceGroupAdjustmentJson.setInstanceGroup(source.getInstanceGroupAdjustment().getInstanceGroup());
                int scaleNumber = source.getInstanceGroupAdjustment().getDesiredCount() - instanceGroup.getNodeCount();
                instanceGroupAdjustmentJson.setScalingAdjustment(scaleNumber);
                updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
            } else {
                throw new BadRequestException(String.format("Group '%s' not available on stack", source.getInstanceGroupAdjustment().getInstanceGroup()));
            }
        }
        return updateStackJson;
    }
}
