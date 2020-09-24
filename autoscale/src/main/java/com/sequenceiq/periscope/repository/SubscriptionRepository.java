package com.sequenceiq.periscope.repository;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.Subscription;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.Optional;

@EntityType(entityClass = Subscription.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface SubscriptionRepository extends CrudRepository<Subscription, Long> {

    Optional<Subscription> findByClientId(@Param("clientId") String clientId);
}
