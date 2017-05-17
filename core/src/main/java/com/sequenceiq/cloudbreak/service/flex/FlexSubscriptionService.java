package com.sequenceiq.cloudbreak.service.flex;

import java.util.List;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.domain.CbUser;
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

    public void delete(FlexSubscription subscription, CbUser user) {
        if (subscription != null) {
            boolean owner = user.getUserId().equals(subscription.getOwner());
            boolean adminInTheAccount = user.getRoles().contains(CbUserRole.ADMIN) && subscription.getAccount().equals(user.getAccount());
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
            throw new CloudbreakServiceException(String.format("Flex subscription could not be found with id: %s", subscription.getId()));
        }
    }

    public void delete(Long id, CbUser user) {
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

    public List<FlexSubscription> findPublicInAccountForUser(CbUser user) {
        LOGGER.info("Looking for public Flex subscriptions for user: {}", user.getUsername());
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return flexRepo.findAllInAccount(user.getAccount());
        } else {
            return flexRepo.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    public void setDefaultFlexSubscription(String name, CbUser cbUser) {
        setFlexSubscriptionFlag(name, cbUser, (flex, flag) -> flex.setDefault(flag));
    }

    public void setUsedForControllerFlexSubscription(String name, CbUser cbUser) {
        setFlexSubscriptionFlag(name, cbUser, (flex, flag) -> flex.setUsedForController(flag));
    }

    private void setFlexSubscriptionFlag(String name, CbUser cbUser, BiConsumer<FlexSubscription, Boolean> setter) {
        List<FlexSubscription> allInAccount = flexRepo.findAllInAccount(cbUser.getAccount());
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
