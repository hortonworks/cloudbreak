package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.Subscription;
import com.sequenceiq.cloudbreak.aspect.DisablePermission;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = Subscription.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisablePermission
public interface SubscriptionRepository extends DisabledBaseRepository<Subscription, Long> {

    List<Subscription> findByClientIdAndEndpoint(String clientId, String endpoint);
}
