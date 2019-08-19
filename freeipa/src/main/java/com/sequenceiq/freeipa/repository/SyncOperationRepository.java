package com.sequenceiq.freeipa.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.repository.BaseJpaRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.entity.SyncOperation;

@Transactional(Transactional.TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface SyncOperationRepository extends BaseJpaRepository<SyncOperation, Long> {

    // TODO restrict this permission. Set Password and User sync should have more
    // restrictive permissions than READ but more permissive than WRITE
    // TODO override other BaseJpaRepository methods for consistent behavior
    @CheckPermission(action = ResourceAction.READ)
    @Override
    SyncOperation save(SyncOperation entity);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM SyncOperation s WHERE s.operationId = :operationId")
    Optional<SyncOperation> findByOperationId(@Param("operationId") String operationId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM SyncOperation s WHERE s.accountId = :accountId AND s.endTime IS NULL")
    List<SyncOperation> findRunningByAccountId(@Param("accountId") String accountId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM SyncOperation s WHERE s.accountId = :accountId AND s.syncOperationType = :syncOperationType AND s.endTime IS NULL")
    List<SyncOperation> findRunningByAccountIdAndType(@Param("accountId") String accountId, @Param("syncOperationType")SyncOperationType syncOperationType);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM SyncOperation s WHERE s.startTime < :startBeforeTime AND s.endTime IS NULL")
    List<SyncOperation> findStaleRunning(@Param("startBeforeTime") Long startBeforeTime);
}
