package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;

@EntityType(entityClass = SmartSenseSubscription.class)
public interface SmartSenseSubscriptionRepository extends CrudRepository<SmartSenseSubscription, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    SmartSenseSubscription findOneById(Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    SmartSenseSubscription findBySubscriptionIdAndAccount(String subscription, String account);

    @PostAuthorize("hasPermission(returnObject,'read')")
    SmartSenseSubscription findByAccountAndOwner(String account, String owner);
}
