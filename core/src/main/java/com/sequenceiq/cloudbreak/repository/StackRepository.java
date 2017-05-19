package com.sequenceiq.cloudbreak.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Stack;

@EntityType(entityClass = Stack.class)
public interface StackRepository extends CrudRepository<Stack, Long> {

    Stack findOne(@Param("id") Long id);

    Stack findById(@Param("id") Long id);

    Stack findByIdLazy(@Param("id") Long id);

    Stack findByAmbari(@Param("ambariIp") String ambariIp);

    Set<Stack> findForUser(@Param("user") String user);

    Set<Stack> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    Set<Stack> findAllInAccount(@Param("account") String account);

    Stack findOneWithLists(@Param("id") Long id);

    List<Stack> findAllStackForTemplate(@Param("id") Long id);

    Stack findStackForCluster(@Param("id") Long id);

    Stack findByIdInAccount(@Param("id") Long id, @Param("account") String account);

    Stack findByNameInAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    Stack findByNameInUser(@Param("name") String name, @Param("owner") String owner);

    Stack findOneByName(@Param("name") String name, @Param("account") String account);

    Stack findByIdWithSecurityConfig(@Param("id") Long id);

    List<Stack> findAllAlive();

    List<Stack> findAllAliveAndProvisioned();

    List<Stack> findByStatuses(@Param("statuses") List<Status> statuses);

    Set<Long> findStacksWithoutEvents();

    Set<Stack> findAliveOnes();

    Long countByFlexSubscription(FlexSubscription flexSubscription);

    Long countByCredential(Credential credential);

    Long countByNetwork(Network network);
}
