package com.sequenceiq.cloudbreak.service.flex;

import java.util.List;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.repository.FlexSubscriptionRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

@Service
public class FlexSubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlexSubscriptionService.class);

    @Inject
    private FlexSubscriptionRepository flexRepo;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private AuthorizationService authorizationService;

    public FlexSubscription create(FlexSubscription subscription) {
        if (!flexRepo.countByNameAndAccount(subscription.getName(), subscription.getAccount()).equals(0L)) {
            throw new BadRequestException(String.format("The name: '%s' has already taken by an other FlexSubscription.", subscription.getName()));
        } else if (!flexRepo.countBySubscriptionId(subscription.getSubscriptionId()).equals(0L)) {
            throw new BadRequestException(String.format("The subscriptionId: '%s' has already taken by an other FlexSubscription.",
                    subscription.getSubscriptionId()));
        }
        subscription = flexRepo.save(subscription);
        LOGGER.info("Flex subscription has been created: {}", subscription);
        return subscription;
    }

    public void delete(FlexSubscription subscription) {
        authorizationService.hasWritePermission(subscription);
        if (!stackRepository.countByFlexSubscription(subscription).equals(0L)) {
            throw new BadRequestException("The given Flex subscription cannot be deleted, there are associated clusters");
        }
        flexRepo.delete(subscription);
        LOGGER.info("Flex subscription has been deleted: {}", subscription);
    }

    public void delete(Long id) {
        FlexSubscription subscription = flexRepo.findById(id);
        delete(subscription);
    }

    public FlexSubscription findById(Long id) {
        LOGGER.info("Looking for Flex subscription with id: {}", id);
        return flexRepo.findOne(id);
    }

    public FlexSubscription findOneById(Long id) {
        LOGGER.info("Looking for one Flex subscription with id: {}", id);
        return flexRepo.findById(id);
    }

    public List<FlexSubscription> findByOwner(String owner) {
        LOGGER.info("Looking for Flex subscriptions for owner: {}", owner);
        return flexRepo.findAllByOwner(owner);
    }

    public FlexSubscription findOneByName(String name) {
        LOGGER.info("Looking for Flex subscription name id: {}", name);
        return flexRepo.findByName(name);
    }

    public FlexSubscription findByNameInAccount(String name, String owner, String account) {
        LOGGER.info("Looking for Flex subscription with name: {}, in account: {}", name, account);
        return flexRepo.findPublicInAccountByNameForUser(name, owner, account);
    }

    public List<FlexSubscription> findPublicInAccountForUser(IdentityUser user) {
        LOGGER.info("Looking for public Flex subscriptions for user: {}", user.getUsername());
        if (user.getRoles().contains(IdentityUserRole.ADMIN)) {
            return flexRepo.findAllByAccount(user.getAccount());
        } else {
            return flexRepo.findAllPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    public void setDefaultFlexSubscription(String name, IdentityUser identityUser) {
        setFlexSubscriptionFlag(name, identityUser, (flex, flag) -> flex.setDefault(flag));
    }

    public void setUsedForControllerFlexSubscription(String name, IdentityUser identityUser) {
        setFlexSubscriptionFlag(name, identityUser, (flex, flag) -> flex.setUsedForController(flag));
    }

    private void setFlexSubscriptionFlag(String name, IdentityUser identityUser, BiConsumer<FlexSubscription, Boolean> setter) {
        List<FlexSubscription> allInAccount = flexRepo.findAllByAccount(identityUser.getAccount());
        if (allInAccount.stream().noneMatch(f -> name.equals(f.getName()))) {
            throw new BadRequestException("Given subscription not found with name: " + name);
        }
        for (FlexSubscription flex : allInAccount) {
            if (name.equals(flex.getName())) {
                setter.accept(flex, true);
            } else {
                setter.accept(flex, false);
            }
        }
        flexRepo.save(allInAccount);
    }
}
