package com.sequenceiq.cloudbreak.service.flex;

import java.util.List;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.repository.FlexSubscriptionRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

@Service
public class FlexSubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlexSubscriptionService.class);

    @Inject
    private FlexSubscriptionRepository flexRepo;

    public FlexSubscription create(FlexSubscription subscription) {
        try {
            subscription = flexRepo.save(subscription);
            LOGGER.info("Flex subscription has been created: {}", subscription);
            return subscription;
        } catch (DataIntegrityViolationException dex) {
            String msg = String.format("The name: '%s' has already taken by an other FlexSubscription.", subscription.getName());
            throw new CloudbreakServiceException(msg, dex);
        }
    }

    public void delete(FlexSubscription subscription, IdentityUser user) {
        if (subscription != null) {
            boolean owner = user.getUserId().equals(subscription.getOwner());
            boolean adminInTheAccount = user.getRoles().contains(IdentityUserRole.ADMIN) && subscription.getAccount().equals(user.getAccount());
            if (owner || adminInTheAccount) {
                try {
                    flexRepo.delete(subscription);
                    LOGGER.info("Flex subscription has been deleted: {}", subscription);
                } catch (DataIntegrityViolationException dex) {
                    throw new CloudbreakServiceException("The given Flex subscription cannot be deleted, there are associated clusters", dex);
                }
            } else {
                String msg = "Only the owner or the account admin has access to delete Flex subscription with id: %s";
                throw new CloudbreakServiceException(String.format(msg, subscription.getId()));
            }
        } else {
            throw new CloudbreakServiceException("Flex subscription not found");
        }
    }

    public void delete(Long id, IdentityUser user) {
        FlexSubscription subscription = flexRepo.findOneById(id);
        delete(subscription, user);
    }

    public FlexSubscription findById(Long id) {
        LOGGER.info("Looking for Flex subscription with id: {}", id);
        return flexRepo.findOne(id);
    }

    public FlexSubscription findOneById(Long id) {
        LOGGER.info("Looking for one Flex subscription with id: {}", id);
        return flexRepo.findOneById(id);
    }

    public List<FlexSubscription> findByOwner(String owner) {
        LOGGER.info("Looking for Flex subscriptions for owner: {}", owner);
        return flexRepo.findByOwner(owner);
    }

    public FlexSubscription findOneByName(String name) {
        LOGGER.info("Looking for Flex subscription name id: {}", name);
        return flexRepo.findOneByName(name);
    }

    public FlexSubscription findByNameInAccount(String name, String owner, String account) {
        LOGGER.info("Looking for Flex subscription with name: {}, in account: {}", name, account);
        return flexRepo.findOneByNameInAccount(name, owner, account);
    }

    public List<FlexSubscription> findPublicInAccountForUser(IdentityUser user) {
        LOGGER.info("Looking for public Flex subscriptions for user: {}", user.getUsername());
        if (user.getRoles().contains(IdentityUserRole.ADMIN)) {
            return flexRepo.findAllInAccount(user.getAccount());
        } else {
            return flexRepo.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    public void setDefaultFlexSubscription(String name, IdentityUser identityUser) {
        setFlexSubscriptionFlag(name, identityUser, (flex, flag) -> flex.setDefault(flag));
    }

    public void setUsedForControllerFlexSubscription(String name, IdentityUser identityUser) {
        setFlexSubscriptionFlag(name, identityUser, (flex, flag) -> flex.setUsedForController(flag));
    }

    private void setFlexSubscriptionFlag(String name, IdentityUser identityUser, BiConsumer<FlexSubscription, Boolean> setter) {
        List<FlexSubscription> allInAccount = flexRepo.findAllInAccount(identityUser.getAccount());
        if (!allInAccount.stream().anyMatch(f -> name.equals(f.getSubscriptionId()))) {
            throw new BadRequestException("Given subscription not found with name: " + name);
        }
        for (FlexSubscription flex : allInAccount) {
            if (name.equals(flex.getSubscriptionId())) {
                setter.accept(flex, true);
            } else {
                setter.accept(flex, false);
            }
        }
        flexRepo.save(allInAccount);
    }
}
