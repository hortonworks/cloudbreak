package com.sequenceiq.periscope.repository;

import java.util.List;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.periscope.domain.Subscription;

@DisableHasPermission
@EntityType(entityClass = Subscription.class)
public interface SubscriptionRepository extends DisabledBaseRepository<Subscription, Long> {

    List<Subscription> findByClientIdAndEndpoint(String clientId, String endpoint);
}
