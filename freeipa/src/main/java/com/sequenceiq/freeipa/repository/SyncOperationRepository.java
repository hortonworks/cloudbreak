package com.sequenceiq.freeipa.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.freeipa.entity.SyncOperation;

@Transactional(Transactional.TxType.REQUIRED)
public interface SyncOperationRepository extends JpaRepository<SyncOperation, Long> {
    @Override
    @Query("SELECT s FROM SyncOperation s WHERE s.id = :id")
    Optional<SyncOperation> findById(@Param("id") Long id);

    @Query("SELECT s FROM SyncOperation s WHERE s.operationId = :operationId")
    Optional<SyncOperation> findByOperationId(@Param("operationId") String operationId);

    @Query("SELECT s FROM SyncOperation s WHERE s.accountId = :accountId AND s.endTime = -1")
    List<SyncOperation> findRunningByAccountId(@Param("accountId") String accountId);
}
