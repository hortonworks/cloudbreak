package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.Subscription;

@EntityType(entityClass = Subscription.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface SubscriptionRepository extends CrudRepository<Subscription, Long> {

    List<Subscription> findByClientIdAndEndpoint(String clientId, String endpoint);
}
