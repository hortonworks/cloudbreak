package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

@EntityType(entityClass = SmartSenseSubscription.class)
public interface SmartSenseSubscriptionRepository extends CrudRepository<SmartSenseSubscription, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    SmartSenseSubscription findOneById(Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    SmartSenseSubscription findBySubscriptionIdAndAccount(String subscription, String account);

    SmartSenseSubscription findByAccountAndOwner(String account, String owner);
}
