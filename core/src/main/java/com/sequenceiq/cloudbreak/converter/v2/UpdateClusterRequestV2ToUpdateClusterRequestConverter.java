package com.sequenceiq.cloudbreak.converter.v2;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJsonV2;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class UpdateClusterRequestV2ToUpdateClusterRequestConverter extends AbstractConversionServiceAwareConverter<UpdateClusterJsonV2, UpdateClusterJson> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateClusterRequestV2ToUpdateClusterRequestConverter.class);

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private StackService stackService;

    @Inject
    private BlueprintService blueprintService;

    @Override
    public UpdateClusterJson convert(UpdateClusterJsonV2 source) {
        UpdateClusterJson updateClusterJson = new UpdateClusterJson();
        updateClusterJson.setStatus(source.getStatus());
        if (source.getScaleRequest() != null) {
            Stack stack = stackService.get(source.getStackId());
            HostGroup hostGroupInClusterByName = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(),
                    source.getScaleRequest().getGroup());
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setHostGroup(source.getScaleRequest().getGroup());
            int scaleNumber = source.getScaleRequest().getDesiredCount() - hostGroupInClusterByName.getConstraint().getHostCount();
            hostGroupAdjustmentJson.setScalingAdjustment(scaleNumber);
            hostGroupAdjustmentJson.setValidateNodeCount(source.getScaleRequest().getValidateNodeCount());
            hostGroupAdjustmentJson.setWithStackUpdate(source.getScaleRequest().getWithStackUpdate());
            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
        }
        if (source.getUpdateAmbariRequest() != null) {
            Blueprint blueprint = blueprintService.get(source.getUpdateAmbariRequest().getBlueprintName(), source.getAccount());
            updateClusterJson.setAmbariStackDetails(source.getUpdateAmbariRequest().getAmbariStackDetails());
            updateClusterJson.setUserNamePasswordJson(source.getUpdateAmbariRequest().getUserNamePasswordJson());
            updateClusterJson.setBlueprintId(blueprint.getId());
            updateClusterJson.setHostgroups(source.getUpdateAmbariRequest().getHostgroups());
            updateClusterJson.setValidateBlueprint(source.getUpdateAmbariRequest().getValidateBlueprint());
        }
        return updateClusterJson;
    }
}
