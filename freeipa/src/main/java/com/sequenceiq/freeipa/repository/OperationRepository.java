package com.sequenceiq.freeipa.repository;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.PageRequest;
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

    /**
     * @param accountId      needed for query optimization as environmentList is not indexed (but accountId is)
     * @param environmentCrn environment crn
     * @param operationType  operation type
     * @param first PageRequest that filters the first result
     */
    @Query("""
            SELECT s FROM Operation s
            WHERE s.accountId = :accountId
                AND (:operationType IS NULL OR s.operationType = :operationType)
                AND s.environmentList LIKE CONCAT('%', :environmentCrn, '%')
            ORDER BY startTime DESC
    """)
    Optional<Operation> findLatestByEnvironmentCrnAndOperationType(
            @Param("accountId") String accountId,
            @Param("environmentCrn") String environmentCrn,
            @Param("operationType") OperationType operationType,
            PageRequest first);

}
