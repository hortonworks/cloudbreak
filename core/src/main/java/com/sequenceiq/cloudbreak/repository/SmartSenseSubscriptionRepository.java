package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;

@EntityType(entityClass = SmartSenseSubscription.class)
public interface SmartSenseSubscriptionRepository extends CrudRepository<SmartSenseSubscription, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    SmartSenseSubscription findOneById(@Param("id") Long id);

    SmartSenseSubscription findBySubscriptionId(@Param("subscriptionId") String subscription, @Param("account") String account);

    List<SmartSenseSubscription> findByOwner(@Param("owner") String owner);
}
