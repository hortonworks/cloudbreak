package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4ViewResponses.blueprintV4ViewResponses;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.BlueprintV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4ViewResponses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ParametersQueryV4Response;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.common.NotificationController;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(Blueprint.class)
public class BlueprintV4Controller extends NotificationController implements BlueprintV4Endpoint {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public BlueprintV4ViewResponses list(Long workspaceId) {
        Workspace workspace = getWorkspace(workspaceId);
        Set<BlueprintV4ViewResponse> blueprints = blueprintService.getAllAvailableViewInWorkspace(workspace)
                .stream()
                .map(blueprint -> conversionService.convert(blueprint, BlueprintV4ViewResponse.class))
                .collect(Collectors.toSet());
        return blueprintV4ViewResponses(blueprints);
    }

    @Override
    public BlueprintV4Response get(Long workspaceId, String name) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(blueprint, BlueprintV4Response.class);
    }

    @Override
    public BlueprintV4Response post(Long workspaceId, BlueprintV4Request request) {
        Blueprint blueprint = conversionService.convert(request, Blueprint.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        blueprint = blueprintService.create(blueprint, workspaceId, user);
        notify(ResourceEvent.BLUEPRINT_CREATED);
        return conversionService.convert(blueprint, BlueprintV4Response.class);
    }

    @Override
    public BlueprintV4Response delete(Long workspaceId, String name) {
        Blueprint deleted = blueprintService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.BLUEPRINT_DELETED);
        return conversionService.convert(deleted, BlueprintV4Response.class);
    }

    @Override
    public BlueprintV4Request getRequest(Long workspaceId, String name) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(blueprint, BlueprintV4Request.class);
    }

    @Override
    public ParametersQueryV4Response getParameters(Long workspaceId, String name) {
        Set<String> customParameters = blueprintService.queryCustomParameters(name, getWorkspace(workspaceId));
        Map<String, String> result = new HashMap<>();
        for (String customParameter : customParameters) {
            result.put(customParameter, "");
        }
        ParametersQueryV4Response parametersQueryV4Response = new ParametersQueryV4Response();
        parametersQueryV4Response.setCustom(result);
        return parametersQueryV4Response;
    }

    private Workspace getWorkspace(Long workspaceId) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        return workspaceService.get(workspaceId, user);
    }
}
