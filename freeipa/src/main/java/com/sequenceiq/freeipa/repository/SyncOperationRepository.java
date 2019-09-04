package com.sequenceiq.freeipa.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.entity.SyncOperation;

@Transactional(Transactional.TxType.REQUIRED)
public interface SyncOperationRepository extends JpaRepository<SyncOperation, Long> {

    @Override
    SyncOperation save(SyncOperation entity);

    @Query("SELECT s FROM SyncOperation s WHERE s.operationId = :operationId")
    Optional<SyncOperation> findByOperationId(@Param("operationId") String operationId);

    @Query("SELECT s FROM SyncOperation s WHERE s.accountId = :accountId AND s.endTime IS NULL")
    List<SyncOperation> findRunningByAccountId(@Param("accountId") String accountId);

    @Query("SELECT s FROM SyncOperation s WHERE s.accountId = :accountId AND s.syncOperationType = :syncOperationType AND s.endTime IS NULL")
    List<SyncOperation> findRunningByAccountIdAndType(@Param("accountId") String accountId, @Param("syncOperationType")SyncOperationType syncOperationType);

    @Query("SELECT s FROM SyncOperation s WHERE s.startTime < :startBeforeTime AND s.endTime IS NULL")
    List<SyncOperation> findStaleRunning(@Param("startBeforeTime") Long startBeforeTime);
}
