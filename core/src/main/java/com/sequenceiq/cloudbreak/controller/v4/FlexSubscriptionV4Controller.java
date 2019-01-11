package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscription.FlexSubscriptionV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscription.requests.FlexSubscriptionV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscription.responses.FlexSubscriptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscription.responses.FlexSubscriptionV4Responses;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.util.ConverterUtil;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(FlexSubscription.class)
public class FlexSubscriptionV4Controller implements FlexSubscriptionV4Endpoint {

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public FlexSubscriptionV4Responses list(Long workspaceId) {
        Set<FlexSubscription> subscriptions = flexSubscriptionService.findAllForUserAndWorkspace(workspaceId);
        return new FlexSubscriptionV4Responses(converterUtil.convertAllAsSet(subscriptions, FlexSubscriptionV4Response.class));
    }

    @Override
    public FlexSubscriptionV4Response get(Long workspaceId, String name) {
        FlexSubscription subscription = flexSubscriptionService.findOneByNameAndWorkspace(name, workspaceId);
        return conversionService.convert(subscription, FlexSubscriptionV4Response.class);
    }

    @Override
    public FlexSubscriptionV4Response create(Long workspaceId, FlexSubscriptionV4Request request) {
        FlexSubscription subscription = flexSubscriptionService.createForLoggedInUser(conversionService.convert(request, FlexSubscription.class), workspaceId);
        return conversionService.convert(subscription, FlexSubscriptionV4Response.class);
    }

    @Override
    public FlexSubscriptionV4Response delete(Long workspaceId, String name) {
        FlexSubscription flexSubscription = flexSubscriptionService.deleteByNameFromWorkspace(name, workspaceId);
        return conversionService.convert(flexSubscription, FlexSubscriptionV4Response.class);
    }

    @Override
    public FlexSubscriptionV4Response setUsedForController(Long workspaceId, String name) {
        FlexSubscription flexSubscription = flexSubscriptionService.setUsedForControllerFlexSubscription(name, workspaceId).get();
        return conversionService.convert(flexSubscription, FlexSubscriptionV4Response.class);
    }

    @Override
    public FlexSubscriptionV4Response setDefault(Long workspaceId, String name) {
        FlexSubscription flexSubscription = flexSubscriptionService.setDefaultFlexSubscription(name, workspaceId).get();
        return conversionService.convert(flexSubscription, FlexSubscriptionV4Response.class);
    }
}
