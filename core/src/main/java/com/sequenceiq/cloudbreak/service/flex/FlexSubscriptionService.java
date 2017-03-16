package com.sequenceiq.cloudbreak.service.flex;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.repository.FlexSubscriptionRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

@Service
public class FlexSubscriptionService {

    @Inject
    private FlexSubscriptionRepository flexRepo;

    public FlexSubscription create(FlexSubscription subscription) {
        return flexRepo.save(subscription);
    }

    public void delete(FlexSubscription subscription, CbUser user) {
        if (subscription != null) {
            boolean owner = user.getUserId().equals(subscription.getOwner());
            boolean adminInTheAccount = user.getRoles().contains(CbUserRole.ADMIN) && subscription.getAccount().equals(user.getAccount());
            if (owner || adminInTheAccount) {
                flexRepo.delete(subscription);
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
        return flexRepo.findOne(id);
    }

    public FlexSubscription findOneById(Long id) {
        return flexRepo.findOneById(id);
    }

    public List<FlexSubscription> findByOwner(String owner) {
        return flexRepo.findByOwner(owner);
    }

    public FlexSubscription findOneByName(String name) {
        return flexRepo.findOneByName(name);
    }

    public FlexSubscription findByNameInAccount(String name, String owner, String account) {
        return flexRepo.findOneByNameInAccount(name, owner, account);
    }

    public List<FlexSubscription> findPublicInAccountForUser(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return flexRepo.findAllInAccount(user.getAccount());
        } else {
            return flexRepo.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }
}
