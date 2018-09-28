package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.FlexSubscriptionV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionRequest;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionResponse;
import com.sequenceiq.cloudbreak.converter.FlexSubscriptionRequestToFlexSubscriptionConverter;
import com.sequenceiq.cloudbreak.converter.FlexSubscriptionToJsonConverter;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.repository.FlexSubscriptionRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Controller
@Transactional(TxType.NEVER)
public class FlexSubscriptionV3Controller implements FlexSubscriptionV3Endpoint, WorkspaceAwareResourceController<FlexSubscription> {

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private FlexSubscriptionRequestToFlexSubscriptionConverter toFlexSubscriptionConverter;

    @Inject
    private FlexSubscriptionToJsonConverter toJsonConverter;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public Set<FlexSubscriptionResponse> listByWorkspace(Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Set<FlexSubscription> subscriptions = flexSubscriptionService.findAllForUserAndWorkspace(user, workspaceId);
        return new HashSet<>(toJsonConverter.convert(subscriptions));
    }

    @Override
    public FlexSubscriptionResponse getByNameInWorkspace(Long workspaceId, String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        FlexSubscription subscription = flexSubscriptionService.findOneByNameAndWorkspace(name, workspaceId, user);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public FlexSubscriptionResponse createInWorkspace(Long workspaceId, FlexSubscriptionRequest request) {
        FlexSubscription subscription = toFlexSubscriptionConverter.convert(request);
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        subscription = flexSubscriptionService.create(subscription, workspaceId, user);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public FlexSubscriptionResponse deleteInWorkspace(Long workspaceId, String name) {
        FlexSubscription flexSubscription = flexSubscriptionService.deleteByNameFromWorkspace(name, workspaceId);
        return toJsonConverter.convert(flexSubscription);
    }

    @Override
    public void setUsedForControllerInWorkspace(Long workspaceId, String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Workspace workspace = workspaceService.get(workspaceId, user);
        flexSubscriptionService.setUsedForControllerFlexSubscription(name, user, workspace);
    }

    @Override
    public void setDefaultInWorkspace(Long workspaceId, String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Workspace workspace = workspaceService.get(workspaceId, user);
        flexSubscriptionService.setDefaultFlexSubscription(name, user, workspace);
    }

    @Override
    public Class<? extends WorkspaceResourceRepository<FlexSubscription, ?>> getWorkspaceAwareResourceRepository() {
        return FlexSubscriptionRepository.class;
    }
}
