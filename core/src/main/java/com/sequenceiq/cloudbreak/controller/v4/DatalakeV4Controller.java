package com.sequenceiq.cloudbreak.controller.v4;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DatalakeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.distrox.v1.distrox.StackOperation;

@Controller
public class DatalakeV4Controller implements DatalakeV4Endpoint {

    @Inject
    private StackOperation stackOperation;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public StackViewV4Responses list(String environmentName) {
        return stackOperation.listByEnvironmentName(workspaceService.getForCurrentUser().getId(), environmentName, StackType.DATALAKE);
    }
}
