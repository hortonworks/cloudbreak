package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.Subscription;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@EntityType(entityClass = Subscription.class)
public interface SubscriptionRepository extends CrudRepository<Subscription, Long> {

    List<Subscription> findByClientIdAndEndpoint(String clientId, String endpoint);
}
