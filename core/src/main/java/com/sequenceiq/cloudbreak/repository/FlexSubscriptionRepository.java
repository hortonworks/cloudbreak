package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;

@EntityType(entityClass = FlexSubscription.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface FlexSubscriptionRepository extends CrudRepository<FlexSubscription, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    FlexSubscription findById(Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    FlexSubscription findByName(String name);

    @PostAuthorize("hasPermission(returnObject,'read')")
    List<FlexSubscription> findAllByOwner(String owner);

    @PostAuthorize("hasPermission(returnObject,'read')")
    List<FlexSubscription> findAllByAccount(String account);

    @PostAuthorize("hasPermission(returnObject,'read')")
    @Query("SELECT f FROM FlexSubscription f WHERE f.name= :name AND ((f.account= :account AND f.publicInAccount= true) OR f.owner= :owner)")
    FlexSubscription findPublicInAccountByNameForUser(@Param("name") String name, @Param("owner") String owner, @Param("account") String account);

    @PostAuthorize("hasPermission(returnObject,'read')")
    @Query("SELECT f FROM FlexSubscription f WHERE (f.account= :account AND f.publicInAccount= true) OR f.owner= :owner")
    List<FlexSubscription> findAllPublicInAccountForUser(@Param("owner") String owner, @Param("account") String account);

    FlexSubscription findFirstByUsedForController(boolean usedForController);

    FlexSubscription findFirstByIsDefault(boolean defaultFlag);

    Long countBySmartSenseSubscription(SmartSenseSubscription smartSenseSubscription);

    Long countByNameAndAccount(String name, String account);

    Long countBySubscriptionId(String subscriptionId);
}
