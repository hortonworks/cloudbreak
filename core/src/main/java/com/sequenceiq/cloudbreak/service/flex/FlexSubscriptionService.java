package com.sequenceiq.cloudbreak.service.flex;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.repository.FlexSubscriptionRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class FlexSubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlexSubscriptionService.class);

    @Inject
    private FlexSubscriptionRepository flexSubscriptionRepository;

    @Inject
    private StackService stackService;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private TransactionService transactionService;

    public FlexSubscription create(FlexSubscription subscription) throws TransactionExecutionException {
        if (!flexSubscriptionRepository.countByNameAndAccount(subscription.getName(), subscription.getAccount()).equals(0L)) {
            throw new BadRequestException(String.format("The name: '%s' has already taken by an other FlexSubscription.", subscription.getName()));
        } else if (!flexSubscriptionRepository.countBySubscriptionId(subscription.getSubscriptionId()).equals(0L)) {
            throw new BadRequestException(String.format("The subscriptionId: '%s' has already taken by an other FlexSubscription.",
                    subscription.getSubscriptionId()));
        }
        return transactionService.required(() -> {
            FlexSubscription updated = flexSubscriptionRepository.save(subscription);

            List<FlexSubscription> allInAccount = flexSubscriptionRepository.findAllByAccount(updated.getAccount());
            setSubscriptionAsDefaultIfNeeded(updated, allInAccount);
            updateSubscriptionsDefaultFlagsIfNeeded(updated, allInAccount);
            LOGGER.info("Flex subscription has been created: {}", updated);
            return updated;
        });
    }

    public void delete(FlexSubscription subscription) {
        if (stackService.countByFlexSubscription(subscription) != 0L) {
            throw new BadRequestException("The given Flex subscription cannot be deleted, there are associated clusters");
        }
        flexSubscriptionRepository.delete(subscription);
        LOGGER.info("Flex subscription has been deleted: {}", subscription);
    }

    public void delete(Long id) {
        FlexSubscription subscription = get(id);
        delete(subscription);
    }

    public List<FlexSubscription> findByOwner(String owner) {
        LOGGER.info("Looking for Flex subscriptions for owner: {}", owner);
        return flexSubscriptionRepository.findAllByOwner(owner);
    }

    public FlexSubscription findOneByName(String name) {
        LOGGER.info("Looking for Flex subscription name id: {}", name);
        return flexSubscriptionRepository.findByName(name);
    }

    public FlexSubscription findByNameInAccount(String name, String owner, String account) {
        LOGGER.info("Looking for Flex subscription with name: {}, in account: {}", name, account);
        return flexSubscriptionRepository.findPublicInAccountByNameForUser(name, owner, account);
    }

    public List<FlexSubscription> findPublicInAccountForUser(IdentityUser user) {
        LOGGER.info("Looking for public Flex subscriptions for user: {}", user.getUsername());
        return user.getRoles().contains(IdentityUserRole.ADMIN) ? flexSubscriptionRepository.findAllByAccount(user.getAccount())
                : flexSubscriptionRepository.findAllPublicInAccountForUser(user.getUserId(), user.getAccount());
    }

    public void setDefaultFlexSubscription(String name, IdentityUser identityUser) {
        setFlexSubscriptionFlag(name, identityUser, FlexSubscription::setDefault);
    }

    public void setUsedForControllerFlexSubscription(String name, IdentityUser identityUser) {
        setFlexSubscriptionFlag(name, identityUser, FlexSubscription::setUsedForController);
    }

    public FlexSubscription get(Long id) {
        return flexSubscriptionRepository.findById(id).orElseThrow(notFound("Flex Subscription", id));
    }

    private void setSubscriptionAsDefaultIfNeeded(FlexSubscription subscription, Collection<FlexSubscription> allInAccount) {
        if (allInAccount.stream().allMatch(subscription::equals)) {
            subscription.setDefault(true);
            subscription.setUsedForController(true);
        }
    }

    private void updateSubscriptionsDefaultFlagsIfNeeded(FlexSubscription subscription, Collection<FlexSubscription> allInAccount) {
        if (subscription.isDefault() || subscription.isUsedForController()) {
            if (subscription.isDefault()) {
                setFlagOnFlexSubscriptionCollection(subscription.getName(), FlexSubscription::setDefault, allInAccount);
            }

            if (subscription.isUsedForController()) {
                setFlagOnFlexSubscriptionCollection(subscription.getName(), FlexSubscription::setUsedForController, allInAccount);
            }
            flexSubscriptionRepository.saveAll(allInAccount);
        }
    }

    private void setFlexSubscriptionFlag(String name, IdentityUser identityUser, BiConsumer<FlexSubscription, Boolean> setter) {
        List<FlexSubscription> allInAccount = flexSubscriptionRepository.findAllByAccount(identityUser.getAccount());
        setFlagOnFlexSubscriptionCollection(name, setter, allInAccount);
        flexSubscriptionRepository.saveAll(allInAccount);
    }

    private void setFlagOnFlexSubscriptionCollection(String name, BiConsumer<FlexSubscription, Boolean> setter, Collection<FlexSubscription> allInAccount) {
        if (allInAccount.stream().noneMatch(f -> name.equals(f.getName()))) {
            throw new BadRequestException("Given subscription not found with name: " + name);
        }
        for (FlexSubscription flex : allInAccount) {
            setter.accept(flex, name.equals(flex.getName()));
        }
    }

    public boolean hasBySmartSenseSubscription(SmartSenseSubscription subscription) {
        return flexSubscriptionRepository.countBySmartSenseSubscription(subscription).equals(0L);
    }

    public FlexSubscription findFirstByUsedForController(boolean usedForController) {
        return flexSubscriptionRepository.findFirstByUsedForController(usedForController);
    }

    public FlexSubscription findFirstByIsDefault(boolean byDefault) {
        return flexSubscriptionRepository.findFirstByIsDefault(byDefault);
    }
}
