package com.sequenceiq.freeipa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.service.model.projection.ResourceCrnAndNameView;
import com.sequenceiq.cloudbreak.structuredevent.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.dto.StackIdWithStatus;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;

@Transactional(Transactional.TxType.REQUIRED)
@EntityType(entityClass = Stack.class)
public interface StackRepository extends AccountAwareResourceRepository<Stack, Long> {

    @Query("SELECT s FROM Stack s WHERE s.terminated = -1")
    List<Stack> findAllRunning();

    @Query("SELECT s FROM Stack s WHERE s.terminated = -1 and s.stackStatus.status in (:statuses)")
    List<Stack> findAllRunningAndStatusIn(@Param("statuses") Collection<Status> statuses);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE s.id= :id ")
    Optional<Stack> findOneWithLists(@Param("id") Long id);

    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId AND s.environmentCrn = :environmentCrn AND s.name = :name AND s.terminated = -1")
    Optional<Stack> findByAccountIdEnvironmentCrnAndName(
            @Param("accountId") String accountId,
            @Param("environmentCrn") String environmentCrn,
            @Param("name") String name);

    @Query("SELECT s FROM Stack s " +
            "LEFT JOIN FETCH s.instanceGroups ig " +
            "LEFT JOIN FETCH ig.instanceMetaData " +
            "WHERE s.accountId = :accountId AND s.terminated = -1")
    List<Stack> findByAccountId(@Param("accountId") String accountId);

    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId AND s.environmentCrn = :environmentCrn AND s.terminated = -1")
    Optional<Stack> findByEnvironmentCrnAndAccountId(@Param("environmentCrn") String environmentCrn, @Param("accountId") String accountId);

    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId AND s.environmentCrn = :environmentCrn")
    Optional<Stack> findByEnvironmentCrnAndAccountIdEvenIfTerminated(@Param("environmentCrn") String environmentCrn, @Param("accountId") String accountId);

    @Query("SELECT s FROM Stack s LEFT JOIN ChildEnvironment c ON c.stack.id = s.id WHERE s.accountId = :accountId "
            + "AND (s.environmentCrn IN :environmentCrns OR c.environmentCrn IN :environmentCrns) AND s.terminated = -1")
    List<Stack> findMultipleByEnvironmentCrnOrChildEnvironmentCrnAndAccountId(
            @Param("environmentCrns") Collection<String> environmentCrns,
            @Param("accountId") String accountId);

    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId AND s.environmentCrn = :environmentCrn AND s.terminated = -1")
    List<Stack> findAllByEnvironmentCrnAndAccountId(@Param("environmentCrn") String environmentCrn, @Param("accountId") String accountId);

    @Query("SELECT s.id FROM Stack s WHERE s.accountId = :accountId AND s.environmentCrn = :environmentCrn AND s.terminated = -1")
    List<Long> findAllIdByEnvironmentCrnAndAccountId(@Param("environmentCrn") String environmentCrn, @Param("accountId") String accountId);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig "
            + "LEFT JOIN FETCH ig.instanceMetaData WHERE s.environmentCrn = :environmentCrn AND s.accountId = :accountId AND s.terminated = -1")
    Optional<Stack> findByEnvironmentCrnAndAccountIdWithList(@Param("environmentCrn") String environmentCrn, @Param("accountId") String accountId);

    @Query("SELECT s FROM Stack s LEFT JOIN ChildEnvironment c ON c.stack.id = s.id LEFT JOIN FETCH s.instanceGroups ig "
            + "LEFT JOIN FETCH ig.instanceMetaData WHERE c.environmentCrn = :environmentCrn AND s.accountId = :accountId AND s.terminated = -1")
    Optional<Stack> findByChildEnvironmentCrnAndAccountIdWithList(@Param("environmentCrn") String environmentCrn, @Param("accountId") String accountId);

    @Query("SELECT new com.sequenceiq.freeipa.dto.StackIdWithStatus(s.id,s.stackStatus.status) FROM Stack s WHERE s.id IN (:ids)")
    List<StackIdWithStatus> findStackStatusesWithoutAuth(@Param("ids") Set<Long> ids);

    @Override
    @Query("SELECT s FROM Stack s WHERE s.id = :id")
    Optional<Stack> findById(@Param("id") Long id);

    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status IN :stackStatuses AND s.terminated = -1 ")
    List<Stack> findAllWithStatuses(@Param("stackStatuses") Collection<Status> stackStatuses);

    @Query("SELECT s FROM Stack s WHERE s.stackStatus.detailedStackStatus IN :detailedStackStatuses AND s.terminated = -1 ")
    List<Stack> findAllWithDetailedStackStatuses(@Param("detailedStackStatuses") Collection<DetailedStackStatus> detailedStackStatuses);

    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId AND s.stackStatus.status IN :stackStatuses AND s.terminated = -1 ")
    List<Stack> findByAccountIdWithStatuses(@Param("accountId") String accountId, @Param("stackStatuses") Collection<Status> stackStatuses);

    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId AND s.environmentCrn IN :environmentCrns " +
            "AND s.stackStatus.status IN :stackStatuses AND s.terminated = -1 ")
    List<Stack> findMultipleByEnvironmentCrnAndAccountIdWithStatuses(
            @Param("environmentCrns") Collection<String> environmentCrns, @Param("accountId") String accountId,
            @Param("stackStatuses") Collection<Status> stackStatuses);

    @Query("SELECT s.environmentCrn FROM Stack s WHERE s.accountId = :accountId AND s.terminated = -1")
    List<String> findAllEnvironmentCrnsByTenantId(@Param("accountId") String accountId);

    @Query("SELECT s.name as name, s.resourceCrn as crn FROM Stack s" +
            " WHERE s.accountId = :accountId AND s.terminated = -1 AND s.environmentCrn IN (:environmentCrns)")
    List<ResourceCrnAndNameView> findNamesByEnvironmentCrnAndAccountId(@Param("environmentCrns") Collection<String> environmentCrns,
            @Param("accountId") String accountId);

    @Query("SELECT s.name as name, s.resourceCrn as crn FROM Stack s" +
            " WHERE s.accountId = :accountId AND s.terminated = -1 AND s.resourceCrn IN (:resourceCrns)")
    List<ResourceCrnAndNameView> findNamesByResourceCrnAndAccountId(@Param("resourceCrns") Collection<String> resourceCrns,
            @Param("accountId") String accountId);

    @Query("SELECT i FROM Stack s JOIN s.image i WHERE (s.terminated = -1 OR s.terminated >= :thresholdTimestamp)")
    List<ImageEntity> findImagesOfAliveStacks(@Param("thresholdTimestamp") long thresholdTimestamp);
}
