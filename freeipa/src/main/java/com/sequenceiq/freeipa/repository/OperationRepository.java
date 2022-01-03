package com.sequenceiq.freeipa.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;

@Transactional(Transactional.TxType.REQUIRED)
public interface OperationRepository extends JpaRepository<Operation, Long> {

    Optional<Operation> findByOperationId(String operationId);

    Optional<Operation> findByOperationIdAndAccountId(String operationId, String accountId);

    @Query("SELECT s FROM Operation s WHERE s.accountId = :accountId AND s.operationType = :operationType AND s.endTime IS NULL")
    List<Operation> findRunningByAccountIdAndType(@Param("accountId") String accountId, @Param("operationType") OperationType operationType);

    @Query("SELECT s FROM Operation s WHERE s.startTime < :startBeforeTime AND s.endTime IS NULL AND s.operationType <> 'UPGRADE'")
    List<Operation> findNonUpgradeStaleRunning(@Param("startBeforeTime") Long startBeforeTime);

    @Query("SELECT s FROM Operation s WHERE s.startTime < :startBeforeTime AND s.endTime IS NULL AND s.operationType = 'UPGRADE'")
    List<Operation> findUpgradeStaleRunning(@Param("startBeforeTime") Long startBeforeTime);
}
