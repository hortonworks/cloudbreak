package com.sequenceiq.cloudbreak.controller.v4;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.google.common.collect.Sets;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.WorkspaceAwareUtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ExposedServiceV4Responses;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;

@Controller
@DisableCheckPermissions
public class WorkspaceAwareUtilV4Controller extends NotificationController implements WorkspaceAwareUtilV4Endpoint {

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

    @Inject
    private CloudbreakRestRequestThreadLocalService threadLocalService;

    @Override
    public ExposedServiceV4Responses getKnoxServices(Long workspaceId, String blueprintName) {
        return new ExposedServiceV4Responses(Sets.newHashSet(serviceEndpointCollector.getKnoxServices(
                threadLocalService.getRequestedWorkspaceId(), blueprintName)));
    }

}
