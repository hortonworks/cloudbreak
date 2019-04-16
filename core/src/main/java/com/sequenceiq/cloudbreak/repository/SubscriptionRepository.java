package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.Subscription;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = Subscription.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface SubscriptionRepository extends DisabledBaseRepository<Subscription, Long> {

    List<Subscription> findByClientIdAndEndpoint(String clientId, String endpoint);

}
