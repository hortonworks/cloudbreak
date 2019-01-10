package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ExposedServiceV4Responses.exposedServiceV4Responses;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.WorkspaceAwareUtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.filter.BlueprintNameV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ExposedServiceV4Responses;
import com.sequenceiq.cloudbreak.controller.common.NotificationController;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.service.cluster.RepositoryConfigValidationService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemSupportMatrixService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Controller
public class WorkspaceAwareUtilV4Controller extends NotificationController implements WorkspaceAwareUtilV4Endpoint {

    @Inject
    private StackMatrixService stackMatrixService;

    @Inject
    private FileSystemSupportMatrixService fileSystemSupportMatrixService;

    @Inject
    private RepositoryConfigValidationService validationService;

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public ExposedServiceV4Responses getKnoxServices(Long workspaceId, BlueprintNameV4Filter blueprintNameV4Filter) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(workspaceId, user);
        return exposedServiceV4Responses(serviceEndpointCollector.getKnoxServices(blueprintNameV4Filter.getBlueprintName(), workspace));
    }

}
