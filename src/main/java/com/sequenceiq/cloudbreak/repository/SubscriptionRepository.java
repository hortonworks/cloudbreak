package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.Subscription;

public interface SubscriptionRepository extends CrudRepository<Subscription, Long> {
}
