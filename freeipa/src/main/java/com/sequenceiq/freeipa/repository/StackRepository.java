package com.sequenceiq.freeipa.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.freeipa.entity.Stack;

@Transactional(Transactional.TxType.REQUIRED)
public interface StackRepository extends CrudRepository<Stack, Long> {

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE s.id= :id ")
    Optional<Stack> findOneWithLists(@Param("id") Long id);

    Optional<Stack> findByNameAndEnvironment(String name, String environment);

    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId and s.environment = :environment ")
    Optional<Stack> findByAccountIdAndEnvironment(String accountId, String environment);

    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId and s.environment = :environment and s.name = :name")
    Optional<Stack> findByAccountIdEnvironmentAndName(
            @Param("accountId") String accountId,
            @Param("environment")  String environment,
            @Param("name") String name);

    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId ")
    Optional<Stack> findByAccountId(String accountId);
}
