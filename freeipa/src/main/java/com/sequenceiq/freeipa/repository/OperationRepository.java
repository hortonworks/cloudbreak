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

    @Override
    Operation save(Operation entity);

    @Query("SELECT s FROM Operation s WHERE s.operationId = :operationId")
    Optional<Operation> findByOperationId(@Param("operationId") String operationId);

    Optional<Operation> findByOperationIdAndAccountId(String operationId, String accountId);

    @Query("SELECT s FROM Operation s WHERE s.accountId = :accountId AND s.endTime IS NULL")
    List<Operation> findRunningByAccountId(@Param("accountId") String accountId);

    @Query("SELECT s FROM Operation s WHERE s.accountId = :accountId AND s.operationType = :operationType AND s.endTime IS NULL")
    List<Operation> findRunningByAccountIdAndType(@Param("accountId") String accountId, @Param("operationType") OperationType operationType);

    @Query("SELECT s FROM Operation s WHERE s.startTime < :startBeforeTime AND s.endTime IS NULL")
    List<Operation> findStaleRunning(@Param("startBeforeTime") Long startBeforeTime);
}
