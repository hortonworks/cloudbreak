package com.sequenceiq.periscope.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.periscope.domain.Subscription;


public interface SubscriptionRepository extends CrudRepository<Subscription, Long> {

    List<Subscription> findByClientIdAndEndpoint(String clientId, String endpoint);

}
