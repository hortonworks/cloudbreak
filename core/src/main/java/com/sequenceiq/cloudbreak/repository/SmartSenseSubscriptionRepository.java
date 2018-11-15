package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = SmartSenseSubscription.class)
@Transactional(Transactional.TxType.REQUIRED)
@HasPermission
public interface SmartSenseSubscriptionRepository extends BaseRepository<SmartSenseSubscription, Long> {

}
