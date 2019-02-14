package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ReinstallV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class ReinstallV4RequestToUpdateClusterV4RequestConverter extends AbstractConversionServiceAwareConverter<ReinstallV4Request, UpdateClusterV4Request> {

    @Inject
    private ClusterDefinitionService clusterDefinitionService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public UpdateClusterV4Request convert(ReinstallV4Request source) {
        UpdateClusterV4Request updateStackJson = new UpdateClusterV4Request();
        updateStackJson.setValidateBlueprint(true);
        updateStackJson.setKerberosPassword(source.getKerberosPassword());
        updateStackJson.setKerberosPrincipal(source.getKerberosPrincipal());
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        ClusterDefinition clusterDefinition = clusterDefinitionService.getByNameForWorkspace(source.getBlueprintName(), workspace);
        updateStackJson.setBlueprintName(clusterDefinition.getName());
        updateStackJson.setStackRepository(source.getStackRepository());
        Set<HostGroupV4Request> hostgroups = new HashSet<>();
        for (InstanceGroupV4Request instanceGroup : source.getInstanceGroups()) {
            HostGroupV4Request hostGroupRequest = new HostGroupV4Request();
            hostGroupRequest.setRecoveryMode(instanceGroup.getRecoveryMode());
            hostGroupRequest.setRecipeNames(instanceGroup.getRecipeNames());
            hostGroupRequest.setName(instanceGroup.getName());
        }
        updateStackJson.setHostgroups(hostgroups);
        return updateStackJson;
    }
}
