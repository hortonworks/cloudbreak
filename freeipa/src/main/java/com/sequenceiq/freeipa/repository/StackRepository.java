package com.sequenceiq.freeipa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.repository.BaseJpaRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.dto.StackIdWithStatus;
import com.sequenceiq.freeipa.entity.Stack;

@Transactional(Transactional.TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface StackRepository extends BaseJpaRepository<Stack, Long> {

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Stack s WHERE s.terminated = -1")
    List<Stack> findAllRunning();

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Stack s WHERE s.terminated = -1 and s.stackStatus.status not in (:statuses)")
    List<Stack> findAllRunningAndStatusNotIn(@Param("statuses") Collection<Status> statuses);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE s.id= :id ")
    Optional<Stack> findOneWithLists(@Param("id") Long id);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId AND s.environmentCrn = :environmentCrn AND s.name = :name AND s.terminated = -1")
    Optional<Stack> findByAccountIdEnvironmentCrnAndName(
            @Param("accountId") String accountId,
            @Param("environmentCrn") String environmentCrn,
            @Param("name") String name);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId AND s.terminated = -1")
    List<Stack> findByAccountId(@Param("accountId") String accountId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId AND s.environmentCrn = :environmentCrn AND s.terminated = -1")
    Optional<Stack> findByEnvironmentCrnAndAccountId(@Param("environmentCrn") String environmentCrn, @Param("accountId") String accountId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId AND s.environmentCrn IN :environmentCrns AND s.terminated = -1")
    List<Stack> findMultipleByEnvironmentCrnAndAccountId(@Param("environmentCrns") Collection<String> environmentCrns, @Param("accountId") String accountId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId AND s.environmentCrn = :environmentCrn AND s.terminated = -1")
    List<Stack> findAllByEnvironmentCrnAndAccountId(@Param("environmentCrn") String environmentCrn, @Param("accountId") String accountId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s.id FROM Stack s WHERE s.accountId = :accountId AND s.environmentCrn = :environmentCrn AND s.terminated = -1")
    List<Long> findAllIdByEnvironmentCrnAndAccountId(@Param("environmentCrn") String environmentCrn, @Param("accountId") String accountId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig "
            + "LEFT JOIN FETCH ig.instanceMetaData WHERE s.environmentCrn = :environmentCrn AND s.accountId = :accountId AND s.terminated = -1")
    Optional<Stack> findByEnvironmentCrnAndAccountIdWithList(@Param("environmentCrn") String environmentCrn, @Param("accountId") String accountId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.childEnvironments c LEFT JOIN FETCH s.instanceGroups ig "
            + "LEFT JOIN FETCH ig.instanceMetaData WHERE c.childEnvironmentCrn = :childEnvironmentCrn AND s.accountId = :accountId AND s.terminated = -1")
    Optional<Stack> findByChildEnvironmentCrnAndAccountIdWithList(@Param("childEnvironmentCrn") String childEnvironmentCrn, @Param("accountId") String accountId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT new com.sequenceiq.freeipa.dto.StackIdWithStatus(s.id,s.stackStatus.status) FROM Stack s WHERE s.id IN (:ids)")
    List<StackIdWithStatus> findStackStatusesWithoutAuth(@Param("ids") Set<Long> ids);

    @CheckPermission(action = ResourceAction.READ)
    @Override
    @Query("SELECT s FROM Stack s WHERE s.id = :id")
    Optional<Stack> findById(@Param("id") Long id);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status IN :stackStatuses AND s.terminated = -1 ")
    List<Stack> findAllWithStatuses(@Param("stackStatuses") Collection<Status> stackStatuses);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId AND s.stackStatus.status IN :stackStatuses AND s.terminated = -1 ")
    List<Stack> findByAccountIdWithStatuses(@Param("accountId") String accountId, @Param("stackStatuses") Collection<Status> stackStatuses);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId AND s.environmentCrn IN :environmentCrns " +
            "AND s.stackStatus.status IN :stackStatuses AND s.terminated = -1 ")
    List<Stack> findMultipleByEnvironmentCrnAndAccountIdWithStatuses(
            @Param("environmentCrns") Collection<String> environmentCrns, @Param("accountId") String accountId,
            @Param("stackStatuses") Collection<Status> stackStatuses);
}
