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
import com.sequenceiq.cloudbreak.api.model.BlueprintViewResponse;
import com.sequenceiq.cloudbreak.api.model.ParametersQueryResponse;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ClusterDefinition.class)
public class BlueprintV3Controller extends NotificationController implements BlueprintV3Endpoint {

    @Inject
    private ClusterDefinitionService clusterDefinitionService;

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
    public Set<BlueprintViewResponse> listByWorkspace(Long workspaceId) {
        Workspace workspace = getWorkspace(workspaceId);
        return clusterDefinitionService.getAllAvailableViewInWorkspace(workspace).stream()
                .map(blueprint -> conversionService.convert(blueprint, BlueprintViewResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public BlueprintResponse getByNameInWorkspace(Long workspaceId, String name) {
        ClusterDefinition clusterDefinition = clusterDefinitionService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(clusterDefinition, BlueprintResponse.class);
    }

    @Override
    public BlueprintResponse createInWorkspace(Long workspaceId, BlueprintRequest request) {
        ClusterDefinition clusterDefinition = conversionService.convert(request, ClusterDefinition.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        clusterDefinition = clusterDefinitionService.create(clusterDefinition, workspaceId, user);
        notify(ResourceEvent.BLUEPRINT_CREATED);
        return conversionService.convert(clusterDefinition, BlueprintResponse.class);
    }

    @Override
    public BlueprintResponse deleteInWorkspace(Long workspaceId, String name) {
        ClusterDefinition deleted = clusterDefinitionService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.BLUEPRINT_DELETED);
        return conversionService.convert(deleted, BlueprintResponse.class);
    }

    @Override
    public BlueprintRequest getRequestFromName(Long workspaceId, String name) {
        ClusterDefinition clusterDefinition = clusterDefinitionService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(clusterDefinition, BlueprintRequest.class);
    }

    @Override
    public ParametersQueryResponse getCustomParameters(Long workspaceId, String name) {
        Set<String> customParameters = clusterDefinitionService.queryCustomParameters(name, getWorkspace(workspaceId));
        Map<String, String> result = new HashMap<>();
        for (String customParameter : customParameters) {
            result.put(customParameter, "");
        }
        ParametersQueryResponse parametersQueryResponse = new ParametersQueryResponse();
        parametersQueryResponse.setCustom(result);
        return parametersQueryResponse;
    }

    private Workspace getWorkspace(Long workspaceId) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        return workspaceService.get(workspaceId, user);
    }
}
