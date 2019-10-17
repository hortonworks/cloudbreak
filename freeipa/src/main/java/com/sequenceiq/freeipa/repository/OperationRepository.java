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
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;

@Transactional(Transactional.TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface OperationRepository extends BaseJpaRepository<Operation, Long> {

    // TODO restrict this permission. Set Password and User sync should have more
    // restrictive permissions than READ but more permissive than WRITE
    // TODO override other BaseJpaRepository methods for consistent behavior
    @CheckPermission(action = ResourceAction.READ)
    @Override
    Operation save(Operation entity);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Operation s WHERE s.operationId = :operationId")
    Optional<Operation> findByOperationId(@Param("operationId") String operationId);

    @CheckPermission(action = ResourceAction.READ)
    Optional<Operation> findByOperationIdAndAccountId(String operationId, String accountId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Operation s WHERE s.accountId = :accountId AND s.endTime IS NULL")
    List<Operation> findRunningByAccountId(@Param("accountId") String accountId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Operation s WHERE s.accountId = :accountId AND s.operationType = :operationType AND s.endTime IS NULL")
    List<Operation> findRunningByAccountIdAndType(@Param("accountId") String accountId, @Param("operationType") OperationType operationType);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Operation s WHERE s.startTime < :startBeforeTime AND s.endTime IS NULL")
    List<Operation> findStaleRunning(@Param("startBeforeTime") Long startBeforeTime);
}
