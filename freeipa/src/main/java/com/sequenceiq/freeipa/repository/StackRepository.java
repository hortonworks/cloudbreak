package com.sequenceiq.freeipa.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.freeipa.entity.Stack;

@Transactional(Transactional.TxType.REQUIRED)
public interface StackRepository extends JpaRepository<Stack, Long> {

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE s.id= :id ")
    Optional<Stack> findOneWithLists(@Param("id") Long id);

    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId and s.environmentCrn = :environmentCrn and s.name = :name")
    Optional<Stack> findByAccountIdEnvironmentCrnAndName(
            @Param("accountId") String accountId,
            @Param("environmentCrn") String environmentCrn,
            @Param("name") String name);

    List<Stack> findByAccountId(String accountId);

    Optional<Stack> findByEnvironmentCrnAndAccountId(String environmentCrn, String accountId);

    List<Stack> findAllByEnvironmentCrnAndAccountId(String environmentCrn, String accountId);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig "
            + "LEFT JOIN FETCH ig.instanceMetaData WHERE s.environmentCrn = :environmentCrn AND s.accountId = :accountId")
    Optional<Stack> findByEnvironmentCrnAndAccountIdWithList(@Param("environmentCrn") String environmentCrn, @Param("accountId") String accountId);
}
