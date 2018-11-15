package com.sequenceiq.cloudbreak.controller;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.FlexSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionRequest;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionResponse;
import com.sequenceiq.cloudbreak.converter.FlexSubscriptionRequestToFlexSubscriptionConverter;
import com.sequenceiq.cloudbreak.converter.FlexSubscriptionToJsonConverter;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Controller
@Transactional(TxType.NEVER)
public class FlexSubscriptionController implements FlexSubscriptionEndpoint {

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    private FlexSubscriptionRequestToFlexSubscriptionConverter toFlexSubscriptionConverter;

    @Inject
    private FlexSubscriptionToJsonConverter toJsonConverter;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public FlexSubscriptionResponse get(Long id) {
        FlexSubscription flexSubscription = flexSubscriptionService.get(id);
        return toJsonConverter.convert(flexSubscription);
    }

    @Override
    public FlexSubscriptionResponse delete(Long id) {
        FlexSubscription flexSubscription = flexSubscriptionService.delete(id);
        return toJsonConverter.convert(flexSubscription);
    }

    @Override
    public FlexSubscriptionResponse deletePublic(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        FlexSubscription flexSubscription = flexSubscriptionService.deleteByNameFromWorkspace(name, workspace.getId());
        return toJsonConverter.convert(flexSubscription);
    }

    @Override
    public FlexSubscriptionResponse deletePrivate(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        FlexSubscription flexSubscription = flexSubscriptionService.deleteByNameFromWorkspace(name, workspace.getId());
        return toJsonConverter.convert(flexSubscription);
    }

    @Override
    public FlexSubscriptionResponse postPublic(FlexSubscriptionRequest flexSubscription) {
        return createFlexSubscription(flexSubscription);
    }

    @Override
    public List<FlexSubscriptionResponse> getPublics() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        Set<FlexSubscription> subscriptions = flexSubscriptionService.findAllForUserAndWorkspace(user, workspace.getId());
        return toJsonConverter.convert(subscriptions);
    }

    @Override
    public FlexSubscriptionResponse getPublic(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        FlexSubscription subscription = flexSubscriptionService.getByNameForWorkspace(name, workspace);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public void setDefaultInAccount(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        flexSubscriptionService.setDefaultFlexSubscription(name, user, workspace);
    }

    @Override
    public void setUsedForControllerInAccount(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        flexSubscriptionService.setUsedForControllerFlexSubscription(name, user, workspace);
    }

    @Override
    public FlexSubscriptionResponse postPrivate(FlexSubscriptionRequest flexSubscription) {
        return createFlexSubscription(flexSubscription);
    }

    @Override
    public List<FlexSubscriptionResponse> getPrivates() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        Set<FlexSubscription> subscriptions = flexSubscriptionService.findAllForUserAndWorkspace(user, workspace.getId());
        return toJsonConverter.convert(subscriptions);
    }

    @Override
    public FlexSubscriptionResponse getPrivate(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        FlexSubscription subscription = flexSubscriptionService.getByNameForWorkspace(name, workspace);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public void setDefaultInAccount(Long id) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        FlexSubscription flexSubscription = flexSubscriptionService.get(id);
        flexSubscriptionService.setDefaultFlexSubscription(flexSubscription.getName(), user, workspace);
    }

    @Override
    public void setUsedForControllerInAccount(Long id) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        FlexSubscription flexSubscription = flexSubscriptionService.get(id);
        flexSubscriptionService.setUsedForControllerFlexSubscription(flexSubscription.getName(), user, workspace);
    }

    private FlexSubscriptionResponse createFlexSubscription(FlexSubscriptionRequest json) {
        FlexSubscription subscription = toFlexSubscriptionConverter.convert(json);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        subscription = flexSubscriptionService.create(subscription, workspace, user);
        return toJsonConverter.convert(subscription);
    }
}
