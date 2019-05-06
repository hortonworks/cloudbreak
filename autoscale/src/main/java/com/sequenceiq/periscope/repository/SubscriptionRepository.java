package com.sequenceiq.periscope.repository;

import java.util.List;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.Subscription;

@DisableHasPermission
@EntityType(entityClass = Subscription.class)
public interface SubscriptionRepository extends DisabledBaseRepository<Subscription, Long> {

    List<Subscription> findByClientIdAndEndpoint(String clientId, String endpoint);
}
