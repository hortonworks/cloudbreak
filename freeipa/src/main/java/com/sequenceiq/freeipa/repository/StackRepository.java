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

    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId and s.environment = :environment and s.name = :name")
    Optional<Stack> findByAccountIdEnvironmentAndName(
            @Param("accountId") String accountId,
            @Param("environment")  String environment,
            @Param("name") String name);

    @Query("SELECT s FROM Stack s WHERE s.environment = :environment")
    Optional<Stack> findByEnvironment(@Param("environment") String environment);
}
