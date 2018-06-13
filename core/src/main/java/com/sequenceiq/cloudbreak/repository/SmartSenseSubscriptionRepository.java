package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;

@EntityType(entityClass = SmartSenseSubscription.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface SmartSenseSubscriptionRepository extends CrudRepository<SmartSenseSubscription, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    SmartSenseSubscription findOneById(Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    SmartSenseSubscription findBySubscriptionIdAndAccount(String subscription, String account);

    SmartSenseSubscription findByAccountAndOwner(String account, String owner);
}
