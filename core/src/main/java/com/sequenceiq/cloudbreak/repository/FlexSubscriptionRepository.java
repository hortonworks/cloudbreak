package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.FlexSubscription;

@EntityType(entityClass = FlexSubscription.class)
public interface FlexSubscriptionRepository extends CrudRepository<FlexSubscription, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    FlexSubscription findOneById(@Param("id") Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    FlexSubscription findOneByName(@Param("name") String name);

    FlexSubscription findOneByNameInAccount(@Param("name") String name, @Param("owner") String owner, @Param("account") String account);

    List<FlexSubscription> findByOwner(@Param("owner") String owner);

    List<FlexSubscription> findPublicInAccountForUser(@Param("owner") String owner, @Param("account") String account);

    List<FlexSubscription> findAllInAccount(@Param("account") String account);
}
