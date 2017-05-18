package com.sequenceiq.cloudbreak.service.smartsense;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.repository.SmartSenseSubscriptionRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

@Service
public class SmartSenseSubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartSenseSubscriptionService.class);

    @Inject
    private SmartSenseSubscriptionRepository repository;

    public SmartSenseSubscription create(SmartSenseSubscription subscription) {
        Iterable<SmartSenseSubscription> subs = repository.findAll();
        if (subs.iterator().hasNext()) {
            throw new BadRequestException("Only one SmartSense subscription is allowed by deployment.");
        }
        try {
            subscription = repository.save(subscription);
            LOGGER.info("SmartSense subscription has been created: {}", subscription);
            return subscription;
        } catch (DataIntegrityViolationException dex) {
            String msg = String.format("The subscription id: '%s' has already taken by an other SmartSenseSubscription.", subscription.getSubscriptionId());
            throw new CloudbreakServiceException(msg, dex);
        }
    }

    public SmartSenseSubscription update(SmartSenseSubscription subscription) {
        return repository.save(subscription);
    }

    public void delete(SmartSenseSubscription subscription, CbUser cbUser) {
        if (subscription != null) {
            boolean owner = cbUser.getUserId().equals(subscription.getOwner());
            boolean adminInTheAccount = cbUser.getRoles().contains(CbUserRole.ADMIN) && subscription.getAccount().equals(cbUser.getAccount());
            if (owner || adminInTheAccount) {
                try {
                    repository.delete(subscription);
                    LOGGER.info("SmartSense subscription has been deleted: {}", subscription);
                } catch (DataIntegrityViolationException divex) {
                    throw new CloudbreakServiceException("Subscription could not be deleted, because it is assigned to Flex subscription(s).", divex);
                }
            } else {
                String msg = "Only the owner or the account admin has access to delete SmartSense subscription with id: %s";
                throw new CloudbreakServiceException(String.format(msg, subscription.getId()));
            }
        } else {
            throw new CloudbreakServiceException("SmartSense subscription not found");
        }
    }

    public void delete(Long id, CbUser cbUser) {
        SmartSenseSubscription subscription = repository.findOneById(id);
        delete(subscription, cbUser);
    }

    public void delete(String subscriptionId, CbUser cbUser) {
        SmartSenseSubscription subscription = repository.findBySubscriptionIdInAccount(subscriptionId, cbUser.getAccount());
        delete(subscription, cbUser);
    }

    public SmartSenseSubscription findById(Long id) {
        LOGGER.info("Looking for SmartSense subscription with id: {}", id);
        return repository.findOne(id);
    }

    public SmartSenseSubscription findOneById(Long id) {
        LOGGER.info("Looking for one SmartSense subscription with id: {}", id);
        return repository.findOneById(id);
    }

    public SmartSenseSubscription findBySubscriptionId(String subscriptionId, String account) {
        LOGGER.info("Looking for SmartSense subscription with subscription id: {} in account: {}", subscriptionId, account);
        return repository.findBySubscriptionId(subscriptionId, account);
    }

    public Optional<SmartSenseSubscription> getOne() {
        LOGGER.info("Get the SmartSense subscription");
        Iterator<SmartSenseSubscription> subscriptions = repository.findAll().iterator();
        if (subscriptions.hasNext()) {
            return Optional.of(subscriptions.next());
        } else {
            return Optional.empty();
        }
    }

    public List<SmartSenseSubscription> findByOwner(String owner) {
        LOGGER.info("Looking for SmartSense subscriptions for owner: {}", owner);
        return repository.findByOwner(owner);
    }
}
