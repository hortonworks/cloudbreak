package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.ConstraintTemplate;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class ConstraintTemplateRequestToConstraintTemplateConverter
        extends AbstractConversionServiceAwareConverter<ConstraintTemplateRequest, ConstraintTemplate> {

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public ConstraintTemplate convert(ConstraintTemplateRequest source) {
        ConstraintTemplate constraintTemplate = new ConstraintTemplate();
        constraintTemplate.setCpu(source.getCpu());
        constraintTemplate.setMemory(source.getMemory());
        constraintTemplate.setDisk(source.getDisk());
        constraintTemplate.setOrchestratorType(source.getOrchestratorType());
        constraintTemplate.setName(source.getName());
        constraintTemplate.setDescription(source.getDescription());
        constraintTemplate.setStatus(ResourceStatus.USER_MANAGED);
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(workspaceId, user);
        constraintTemplate.setWorkspace(workspace);
        return constraintTemplate;
    }

}
