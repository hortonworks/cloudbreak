package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscriptions.FlexSubscriptionV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscriptions.requests.FlexSubscriptionV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscriptions.responses.FlexSubscriptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscriptions.responses.FlexSubscriptionV4Responses;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(FlexSubscription.class)
public class FlexSubscriptionV4Controller implements FlexSubscriptionV4Endpoint {

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public FlexSubscriptionV4Responses list(Long workspaceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Set<FlexSubscription> subscriptions = flexSubscriptionService.findAllForUserAndWorkspace(user, workspaceId);
        Set<FlexSubscriptionV4Response> responses = subscriptions
                .stream()
                .map(subscription -> conversionService.convert(subscriptions, FlexSubscriptionV4Response.class))
                .collect(Collectors.toSet());
        return FlexSubscriptionV4Responses.responses(responses);
    }

    @Override
    public FlexSubscriptionV4Response get(Long workspaceId, String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        FlexSubscription subscription = flexSubscriptionService.findOneByNameAndWorkspace(name, workspaceId, user);
        return conversionService.convert(subscription, FlexSubscriptionV4Response.class);
    }

    @Override
    public FlexSubscriptionV4Response create(Long workspaceId, FlexSubscriptionV4Request request) {
        FlexSubscription subscription = conversionService.convert(request, FlexSubscription.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        subscription = flexSubscriptionService.create(subscription, workspaceId, user);
        return conversionService.convert(subscription, FlexSubscriptionV4Response.class);
    }

    @Override
    public FlexSubscriptionV4Response delete(Long workspaceId, String name) {
        FlexSubscription flexSubscription = flexSubscriptionService.deleteByNameFromWorkspace(name, workspaceId);
        return conversionService.convert(flexSubscription, FlexSubscriptionV4Response.class);
    }

    @Override
    public FlexSubscriptionV4Response setUsedForController(Long workspaceId, String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(workspaceId, user);
        FlexSubscription flexSubscription = flexSubscriptionService.setUsedForControllerFlexSubscription(name, user, workspace).get();
        return conversionService.convert(flexSubscription, FlexSubscriptionV4Response.class);
    }

    @Override
    public FlexSubscriptionV4Response setDefault(Long workspaceId, String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(workspaceId, user);
        FlexSubscription flexSubscription = flexSubscriptionService.setDefaultFlexSubscription(name, user, workspace).get();
        return conversionService.convert(flexSubscription, FlexSubscriptionV4Response.class);
    }
}
