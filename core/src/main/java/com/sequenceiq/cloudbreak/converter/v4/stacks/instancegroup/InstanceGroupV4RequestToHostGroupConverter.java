package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class InstanceGroupV4RequestToHostGroupConverter extends AbstractConversionServiceAwareConverter<InstanceGroupV4Request, HostGroup> {

    @Inject
    private RecipeService recipeService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public HostGroup convert(InstanceGroupV4Request source) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(source.getName());
        hostGroup.setRecoveryMode(source.getRecoveryMode());
        Set<String> recipeNames = source.getRecipeNames();
        if (!CollectionUtils.isEmpty(recipeNames)) {
            hostGroup.setRecipes(recipeService.getRecipesByNamesForWorkspace(workspace, recipeNames));
        }
        return hostGroup;
    }
}
