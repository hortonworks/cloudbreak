package com.sequenceiq.cloudbreak.controller.v4;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DatalakeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.distrox.v1.distrox.StackOperations;

@Controller
@DisableCheckPermissions
public class DatalakeV4Controller implements DatalakeV4Endpoint {

    @Lazy
    @Inject
    private StackOperations stackOperations;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public StackViewV4Responses list(String environmentName, String environmentCrn) {
        if (StringUtils.isNotEmpty(environmentCrn)) {
            return stackOperations.listByEnvironmentCrn(workspaceService.getForCurrentUser().getId(), environmentCrn, List.of(StackType.DATALAKE));
        }
        if (StringUtils.isNotEmpty(environmentName)) {
            return stackOperations.listByEnvironmentName(workspaceService.getForCurrentUser().getId(), environmentName, List.of(StackType.DATALAKE));
        }
        throw new BadRequestException("Either environment or environmentCrn query parameter should be set.");
    }
}
