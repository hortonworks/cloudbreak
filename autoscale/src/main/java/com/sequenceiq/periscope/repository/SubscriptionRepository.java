package com.sequenceiq.periscope.repository;

import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.Subscription;

@EntityType(entityClass = Subscription.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface SubscriptionRepository extends CrudRepository<Subscription, Long> {

    Optional<Subscription> findByClientId(@Param("clientId") String clientId);
}
