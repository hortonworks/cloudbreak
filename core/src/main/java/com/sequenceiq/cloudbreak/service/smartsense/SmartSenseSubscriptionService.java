package com.sequenceiq.cloudbreak.service.smartsense;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.repository.SmartSenseSubscriptionRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Service
public class SmartSenseSubscriptionService {

    @Inject
    private SmartSenseSubscriptionRepository repository;

    public SmartSenseSubscription create(SmartSenseSubscription subscription) {
        List<SmartSenseSubscription> byOwner = repository.findByOwner(subscription.getOwner());
        if (!byOwner.isEmpty()) {
            throw new DuplicateKeyValueException(APIResourceType.SMARTSENSE_SUBSCRIPTION, "Only one SmartSense subscription is allowed by user.");
        }
        return repository.save(subscription);
    }

    public SmartSenseSubscription update(SmartSenseSubscription subscription) {
        return repository.save(subscription);
    }

    public void delete(SmartSenseSubscription subscription, CbUser cbUser) {
        if (subscription != null) {
            boolean owner = cbUser.getUserId().equals(subscription.getOwner());
            boolean adminInTheAccount = cbUser.getRoles().contains(CbUserRole.ADMIN) && subscription.getAccount().equals(cbUser.getAccount());
            if (owner || adminInTheAccount) {
                repository.delete(subscription);
            } else {
                String msg = "Only the owner or the account admin has access to delete SmartSense subscription with id: %s";
                throw new CloudbreakServiceException(String.format(msg, subscription.getId()));
            }
        } else {
            throw new CloudbreakServiceException(String.format("SmartSense subscription could not be found with id: %s", subscription.getId()));
        }
    }

    public void delete(Long id, CbUser cbUser) {
        SmartSenseSubscription subscription = repository.findOneById(id);
        delete(subscription, cbUser);
    }

    public SmartSenseSubscription findById(Long id) {
        return repository.findOne(id);
    }

    public SmartSenseSubscription findOneById(Long id) {
        return repository.findOneById(id);
    }

    public SmartSenseSubscription findBySubscriptionId(String subscriptionId, String account) {
        return repository.findBySubscriptionId(subscriptionId, account);
    }

    public List<SmartSenseSubscription> findByOwner(String owner) {
        return repository.findByOwner(owner);
    }
}
