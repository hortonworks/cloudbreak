package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = FlexSubscription.class)
@Transactional(Transactional.TxType.REQUIRED)
@HasPermission
public interface FlexSubscriptionRepository extends BaseRepository<FlexSubscription, Long> {

    FlexSubscription findByName(String name);

    List<FlexSubscription> findAllByOwner(String owner);

    List<FlexSubscription> findAllByAccount(String account);

    @Query("SELECT f FROM FlexSubscription f WHERE f.name= :name AND ((f.account= :account AND f.publicInAccount= true) OR f.owner= :owner)")
    FlexSubscription findPublicInAccountByNameForUser(@Param("name") String name, @Param("owner") String owner, @Param("account") String account);

    @Query("SELECT f FROM FlexSubscription f WHERE (f.account= :account AND f.publicInAccount= true) OR f.owner= :owner")
    List<FlexSubscription> findAllPublicInAccountForUser(@Param("owner") String owner, @Param("account") String account);

    FlexSubscription findFirstByUsedForController(boolean usedForController);

    FlexSubscription findFirstByIsDefault(boolean defaultFlag);

    Long countBySmartSenseSubscription(SmartSenseSubscription smartSenseSubscription);

    Long countByNameAndAccount(String name, String account);

    Long countBySubscriptionId(String subscriptionId);
}
