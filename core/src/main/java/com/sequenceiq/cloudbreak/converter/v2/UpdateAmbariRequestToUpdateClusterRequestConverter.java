package com.sequenceiq.cloudbreak.converter.v2;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.api.model.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.ReinstallRequestV2;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;

@Component
public class UpdateAmbariRequestToUpdateClusterRequestConverter extends AbstractConversionServiceAwareConverter<ReinstallRequestV2, UpdateClusterJson> {

    @Inject
    private BlueprintRepository blueprintRepository;

    @Override
    public UpdateClusterJson convert(ReinstallRequestV2 source) {
        UpdateClusterJson updateStackJson = new UpdateClusterJson();
        updateStackJson.setValidateBlueprint(true);
        Blueprint blueprint = blueprintRepository.findOneByName(source.getBlueprintName(), source.getAccount());
        if (blueprint != null) {
            updateStackJson.setBlueprintId(blueprint.getId());
            updateStackJson.setAmbariStackDetails(source.getAmbariStackDetails());
            Set<HostGroupRequest> hostgroups = new HashSet<>();
            for (InstanceGroupV2Request instanceGroupV2Request : source.getInstanceGroups()) {
                HostGroupRequest hostGroupRequest = new HostGroupRequest();
                hostGroupRequest.setRecoveryMode(instanceGroupV2Request.getRecoveryMode());
                hostGroupRequest.setRecipeNames(instanceGroupV2Request.getRecipeNames());
                hostGroupRequest.setName(instanceGroupV2Request.getGroup());
                ConstraintJson constraintJson = new ConstraintJson();
                constraintJson.setHostCount(instanceGroupV2Request.getNodeCount());
                constraintJson.setInstanceGroupName(instanceGroupV2Request.getGroup());
                hostGroupRequest.setConstraint(constraintJson);
                hostgroups.add(hostGroupRequest);
            }
            updateStackJson.setHostgroups(hostgroups);
        } else {
            throw new BadRequestException(String.format("Blueprint '%s' not available", source.getBlueprintName()));
        }
        return updateStackJson;
    }
}
