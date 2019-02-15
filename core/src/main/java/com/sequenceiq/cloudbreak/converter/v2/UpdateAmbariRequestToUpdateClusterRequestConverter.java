package com.sequenceiq.cloudbreak.converter.v2;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.api.model.ReinstallRequestV2;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class UpdateAmbariRequestToUpdateClusterRequestConverter extends AbstractConversionServiceAwareConverter<ReinstallRequestV2, UpdateClusterJson> {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public UpdateClusterJson convert(ReinstallRequestV2 source) {
        UpdateClusterJson updateStackJson = new UpdateClusterJson();
        updateStackJson.setValidateBlueprint(true);
        updateStackJson.setKerberosPassword(source.getKerberosPassword());
        updateStackJson.setKerberosPrincipal(source.getKerberosPrincipal());
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        Blueprint blueprint = blueprintService.getByNameForWorkspace(source.getBlueprintName(), workspace);
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
        return updateStackJson;
    }
}
