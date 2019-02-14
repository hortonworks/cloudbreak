package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.BlueprintEndpoint;
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.init.clusterdefinition.ClusterDefinitionLoaderService;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Controller
@Transactional(TxType.NEVER)
public class BlueprintController extends NotificationController implements BlueprintEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintController.class);

    @Autowired
    private ClusterDefinitionService clusterDefinitionService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private ClusterDefinitionLoaderService clusterDefinitionLoaderService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public BlueprintResponse get(Long id) {
        return conversionService.convert(clusterDefinitionService.get(id), BlueprintResponse.class);
    }

    @Override
    public void delete(Long id) {
        ClusterDefinition deleted = clusterDefinitionService.delete(id);
        notify(ResourceEvent.RECIPE_DELETED);
        conversionService.convert(deleted, BlueprintResponse.class);
    }

    @Override
    public BlueprintResponse postPublic(BlueprintRequest request) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return createInWorkspace(request, user, workspace);
    }

    @Override
    public BlueprintResponse postPrivate(BlueprintRequest request) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return createInWorkspace(request, user, workspace);
    }

    @Override
    public Set<BlueprintResponse> getPrivates() {
        return listForUsersDefaultWorkspace();
    }

    @Override
    public Set<BlueprintResponse> getPublics() {
        return listForUsersDefaultWorkspace();
    }

    @Override
    public BlueprintResponse getPrivate(String name) {
        return getBlueprintResponse(name);
    }

    @Override
    public BlueprintResponse getPublic(String name) {
        return getBlueprintResponse(name);
    }

    @Override
    public void deletePublic(String name) {
        deleteInDefaultWorkspace(name);
    }

    @Override
    public BlueprintRequest getRequestfromId(Long id) {
        ClusterDefinition clusterDefinition = clusterDefinitionService.get(id);
        return conversionService.convert(clusterDefinition, BlueprintRequest.class);
    }

    @Override
    public void deletePrivate(String name) {
        deleteInDefaultWorkspace(name);
    }

    private BlueprintResponse getBlueprintResponse(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return conversionService.convert(clusterDefinitionService.getByNameForWorkspace(name, workspace), BlueprintResponse.class);
    }

    private Set<BlueprintResponse> listForUsersDefaultWorkspace() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return clusterDefinitionService.getAllAvailableInWorkspace(workspace).stream()
                .map(blueprint -> conversionService.convert(blueprint, BlueprintResponse.class))
                .collect(Collectors.toSet());
    }

    private void deleteInDefaultWorkspace(String name) {
        executeAndNotify(identityUser -> clusterDefinitionService.deleteByNameFromWorkspace(name, restRequestThreadLocalService.getRequestedWorkspaceId()),
                ResourceEvent.BLUEPRINT_DELETED);
    }

    private BlueprintResponse createInWorkspace(BlueprintRequest request, User user, Workspace workspace) {
        ClusterDefinition clusterDefinition = conversionService.convert(request, ClusterDefinition.class);
        clusterDefinition = clusterDefinitionService.create(clusterDefinition, workspace, user);
        return notifyAndReturn(clusterDefinition, ResourceEvent.BLUEPRINT_CREATED);
    }

    private BlueprintResponse notifyAndReturn(ClusterDefinition clusterDefinition, ResourceEvent resourceEvent) {
        notify(resourceEvent);
        return conversionService.convert(clusterDefinition, BlueprintResponse.class);
    }
}
