package com.sequenceiq.cloudbreak.controller;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v1.SmartSenseSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.model.SmartSenseSubscriptionJson;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.converter.SmartSenseSubscriptionRequestToSmartSenseSubscriptionConverter;
import com.sequenceiq.cloudbreak.converter.SmartSenseSubscriptionToSmartSenseSubscriptionJsonConverter;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class SmartSenseSubscriptionController implements SmartSenseSubscriptionEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartSenseSubscriptionController.class);

    @Inject
    private SmartSenseSubscriptionService smartSenseSubService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private SmartSenseSubscriptionToSmartSenseSubscriptionJsonConverter toJsonConverter;

    @Inject
    private SmartSenseSubscriptionRequestToSmartSenseSubscriptionConverter toSmartSenseSubscriptionConverter;

    @Override
    public SmartSenseSubscriptionJson get() {
        IdentityUser cbUser = authenticatedUserService.getCbUser();
        SmartSenseSubscription subscription = smartSenseSubService.getDefaultForUser(cbUser);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public SmartSenseSubscriptionJson get(Long id) {
        SmartSenseSubscription subscription = smartSenseSubService.findOneById(id);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public void delete(Long id) {
        smartSenseSubService.delete(id);
    }

    @Override
    public void deletePublic(String subscriptionId) {
        IdentityUser cbUser = authenticatedUserService.getCbUser();
        smartSenseSubService.delete(subscriptionId, cbUser);
    }

    @Override
    public void deletePrivate(String subscriptionId) {
        IdentityUser cbUser = authenticatedUserService.getCbUser();
        smartSenseSubService.delete(subscriptionId, cbUser);
    }

    @Override
    public SmartSenseSubscriptionJson postPublic(SmartSenseSubscriptionJson smartSenseSubscriptionJson) {
        return createSmartSenseSubscription(smartSenseSubscriptionJson, true);
    }

    @Override
    public List<SmartSenseSubscriptionJson> getPublics() {
        List<SmartSenseSubscription> result = Lists.newArrayList();
        smartSenseSubService.getDefault().ifPresent(result::add);
        return toJsonConverter.convert(result);
    }

    @Override
    public SmartSenseSubscriptionJson postPrivate(SmartSenseSubscriptionJson smartSenseSubscriptionJson) {
        return createSmartSenseSubscription(smartSenseSubscriptionJson, false);
    }

    @Override
    public List<SmartSenseSubscriptionJson> getPrivates() {
        return getPublics();
    }

    private SmartSenseSubscriptionJson createSmartSenseSubscription(SmartSenseSubscriptionJson json, boolean publicInAccount) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        SmartSenseSubscription subscription = toSmartSenseSubscriptionConverter.convert(json);
        subscription.setAccount(identityUser.getAccount());
        subscription.setOwner(identityUser.getUserId());
        subscription.setPublicInAccount(publicInAccount);
        subscription = smartSenseSubService.create(subscription);
        return toJsonConverter.convert(subscription);
    }

}
