package com.sequenceiq.cloudbreak.controller;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.FlexSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionRequest;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.converter.FlexSubscriptionRequestToFlexSubscriptionConverter;
import com.sequenceiq.cloudbreak.converter.FlexSubscriptionToJsonConverter;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;

@Controller
@Transactional(TxType.NEVER)
public class FlexSubscriptionController implements FlexSubscriptionEndpoint {

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private FlexSubscriptionRequestToFlexSubscriptionConverter toFlexSubscriptionConverter;

    @Inject
    private FlexSubscriptionToJsonConverter toJsonConverter;

    @Override
    public FlexSubscriptionResponse get(Long id) {
        FlexSubscription flexSubscription = flexSubscriptionService.get(id);
        return toJsonConverter.convert(flexSubscription);
    }

    @Override
    public void delete(Long id) {
        flexSubscriptionService.delete(id);
    }

    @Override
    public void deletePublic(String name) {
        IdentityUser cbUser = authenticatedUserService.getCbUser();
        FlexSubscription subscription = flexSubscriptionService.findByNameInAccount(name, cbUser.getUserId(), cbUser.getAccount());
        flexSubscriptionService.delete(subscription);
    }

    @Override
    public void deletePrivate(String name) {
        FlexSubscription subscription = flexSubscriptionService.findOneByName(name);
        flexSubscriptionService.delete(subscription);
    }

    @Override
    public FlexSubscriptionResponse postPublic(FlexSubscriptionRequest flexSubscription) {
        try {
            return createFlexSubscription(flexSubscription, true);
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    @Override
    public List<FlexSubscriptionResponse> getPublics() {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        List<FlexSubscription> subscriptions = flexSubscriptionService.findPublicInAccountForUser(identityUser);
        return toJsonConverter.convert(subscriptions);
    }

    @Override
    public FlexSubscriptionResponse getPublic(String name) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        FlexSubscription subscription = flexSubscriptionService.findByNameInAccount(name, identityUser.getUserId(), identityUser.getAccount());
        return toJsonConverter.convert(subscription);
    }

    @Override
    public void setDefaultInAccount(String name) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        flexSubscriptionService.setDefaultFlexSubscription(name, identityUser);
    }

    @Override
    public void setUsedForControllerInAccount(String name) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        flexSubscriptionService.setUsedForControllerFlexSubscription(name, identityUser);
    }

    @Override
    public FlexSubscriptionResponse postPrivate(FlexSubscriptionRequest flexSubscription) {
        try {
            return createFlexSubscription(flexSubscription, false);
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    @Override
    public List<FlexSubscriptionResponse> getPrivates() {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        List<FlexSubscription> subscriptions = flexSubscriptionService.findByOwner(identityUser.getUserId());
        return toJsonConverter.convert(subscriptions);
    }

    @Override
    public FlexSubscriptionResponse getPrivate(String name) {
        FlexSubscription subscription = flexSubscriptionService.findOneByName(name);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public void setDefaultInAccount(Long id) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        FlexSubscription flexSubscription = flexSubscriptionService.get(id);
        flexSubscriptionService.setDefaultFlexSubscription(flexSubscription.getName(), identityUser);
    }

    @Override
    public void setUsedForControllerInAccount(Long id) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        FlexSubscription flexSubscription = flexSubscriptionService.get(id);
        flexSubscriptionService.setUsedForControllerFlexSubscription(flexSubscription.getName(), identityUser);
    }

    private FlexSubscriptionResponse createFlexSubscription(FlexSubscriptionRequest json, boolean publicInAccount) throws TransactionExecutionException {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        FlexSubscription subscription = toFlexSubscriptionConverter.convert(json);
        subscription.setAccount(identityUser.getAccount());
        subscription.setOwner(identityUser.getUserId());
        subscription.setPublicInAccount(publicInAccount);
        subscription = flexSubscriptionService.create(subscription);
        return toJsonConverter.convert(subscription);
    }
}
