package com.sequenceiq.cloudbreak.controller;

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

import com.sequenceiq.cloudbreak.api.endpoint.v3.BlueprintV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.ParametersQueryResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Controller
@Transactional(TxType.NEVER)
public class BlueprintV3Controller extends NotificationController implements BlueprintV3Endpoint {

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
    public Set<BlueprintResponse> listByWorkspace(Long workspaceId) {
        Workspace workspace = getWorkspace(workspaceId);
        return blueprintService.getAllAvailableInWorkspace(workspace).stream()
                .map(blueprint -> conversionService.convert(blueprint, BlueprintResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public BlueprintResponse getByNameInWorkspace(Long workspaceId, String name) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(blueprint, BlueprintResponse.class);
    }

    @Override
    public BlueprintResponse createInWorkspace(Long workspaceId, BlueprintRequest request) {
        Blueprint blueprint = conversionService.convert(request, Blueprint.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        blueprint = blueprintService.create(blueprint, workspaceId, user);
        notify(ResourceEvent.BLUEPRINT_CREATED);
        return conversionService.convert(blueprint, BlueprintResponse.class);
    }

    @Override
    public BlueprintResponse deleteInWorkspace(Long workspaceId, String name) {
        Blueprint deleted = blueprintService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.BLUEPRINT_DELETED);
        return conversionService.convert(deleted, BlueprintResponse.class);
    }

    @Override
    public BlueprintRequest getRequestFromName(Long workspaceId, String name) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(blueprint, BlueprintRequest.class);
    }

    @Override
    public ParametersQueryResponse getCustomParameters(Long workspaceId, String name) {
        Set<String> customParameters = blueprintService.queryCustomParameters(name, getWorkspace(workspaceId));
        Map<String, String> result = new HashMap<>();
        for (String customParameter : customParameters) {
            result.put(customParameter, "");
        }
        ParametersQueryResponse parametersQueryResponse = new ParametersQueryResponse();
        parametersQueryResponse.setCustom(result);
        return parametersQueryResponse;
    }

    private Workspace getWorkspace(Long workspaceId) {
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        User user = userService.getOrCreate(identityUser);
        return workspaceService.get(workspaceId, user);
    }
}
