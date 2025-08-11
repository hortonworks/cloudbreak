package com.sequenceiq.freeipa.repository;

import static jakarta.transaction.Transactional.TxType.REQUIRED;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.service.model.projection.ResourceCrnAndNameView;
import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.dal.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.dto.StackIdWithStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.projection.StackUserSyncView;

@Transactional(REQUIRED)
@EntityType(entityClass = Stack.class)
public interface StackRepository extends AccountAwareResourceRepository<Stack, Long>, JobResourceRepository<Stack, Long> {

    @Override
    @Query("SELECT s FROM Stack s WHERE s.id = :id")
    Optional<Stack> findById(@Param("id") Long id);

    @Query("SELECT s.id as localId, s.resourceCrn as remoteResourceId, s.name as name, s.cloudPlatform as provider " +
            "FROM Stack s " +
            "WHERE s.id = :resourceId")
    Optional<JobResource> getJobResource(@Param("resourceId") Long resourceId);

    @Query("SELECT s.id as id, s.resourceCrn as resourceCrn, s.name as name, s.environmentCrn as environmentCrn " +
            "FROM Stack s " +
            "WHERE s.terminated = -1 " +
            "AND s.resourceCrn = :resourceCrn")
    Optional<ResourceBasicView> findResourceBasicViewByResourceCrn(@Param("resourceCrn") String resourceCrn);

    @Query("SELECT s.id as id, s.resourceCrn as resourceCrn, s.name as name, s.environmentCrn as environmentCrn " +
            "FROM Stack s " +
            "WHERE s.terminated = -1 " +
            "AND s.resourceCrn in (:resourceCrns)")
    List<ResourceBasicView> findAllResourceBasicViewByResourceCrns(@Param("resourceCrns") Collection<String> resourceCrns);

    @Query("SELECT s.id as id, s.resourceCrn as resourceCrn, s.name as name, s.environmentCrn as environmentCrn " +
            "FROM Stack s " +
            "WHERE s.terminated = -1 " +
            "AND s.name = :name " +
            "AND s.accountId = :accountId")
    Optional<ResourceBasicView> findResourceBasicViewByNameAndAccountId(@Param("name") String name, @Param("accountId") String accountId);

    @Query("SELECT s.id as id, s.resourceCrn as resourceCrn, s.name as name, s.environmentCrn as environmentCrn " +
            "FROM Stack s " +
            "WHERE s.terminated = -1 " +
            "AND s.name in (:names) " +
            "AND s.accountId = :accountId")
    List<ResourceBasicView> findAllResourceBasicViewByNamesAndAccountId(@Param("names") Collection<String> names, @Param("accountId") String accountId);

    @Query("SELECT s FROM Stack s WHERE s.terminated = -1")
    List<Stack> findAllRunning();

    @Query("""
            SELECT s.id as localId, s.resourceCrn as remoteResourceId, s.name as name, s.cloudPlatform as provider
            FROM Stack s
            WHERE s.terminated = -1
                AND s.stackStatus.status NOT IN (:statuses)
    """)
    List<JobResource> findAllRunningAndStatusNotIn(@Param("statuses") Collection<Status> statuses);

    @Query("""
            SELECT s.id as localId, s.resourceCrn as remoteResourceId, s.name as name, s.cloudPlatform as provider
            FROM CrossRealmTrust t
            LEFT JOIN t.stack s
            WHERE s.terminated = -1
                AND s.stackStatus.status NOT IN (:statuses)
                AND t.trustStatus IN (:trustStatuses)
    """)
    List<JobResource> findAllRunningWithCrossRealmTrustAndStatusNotIn(
            @Param("statuses") Collection<Status> statuses,
            @Param("trustStatuses") Collection<TrustStatus> trustStatuses);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE s.id= :id ")
    Optional<Stack> findOneWithLists(@Param("id") Long id);

    @Query("SELECT s FROM Stack s "
            + "LEFT JOIN FETCH s.instanceGroups ig "
            + "LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.accountId = :accountId AND s.environmentCrn = :environmentCrn AND s.resourceCrn = :resourceCrn ")
    Optional<Stack> findByAccountIdEnvironmentCrnAndCrnWithListsEvenIfTerminated(
            @Param("environmentCrn") String environmentCrn,
            @Param("accountId") String accountId,
            @Param("resourceCrn") String resourceCrn);

    @Query("SELECT new com.sequenceiq.freeipa.entity.projection.StackUserSyncView(s.id, s.resourceCrn, s.name, s.environmentCrn, s.accountId, s.cloudPlatform, "
            + "ss.status) "
            + "FROM Stack s "
            + "LEFT JOIN StackStatus ss ON s.stackStatus.id = ss.id "
            + "WHERE s.accountId = :accountId "
            + "AND s.terminated = -1 "
            + "AND ss.detailedStackStatus IN :statuses")
    Set<StackUserSyncView> findUserSyncViewByAccountId(@Param("accountId") String accountId, @Param("statuses") Collection<DetailedStackStatus> statuses);

    @Query("SELECT s.resourceCrn as resourceCrn, s.id as id, s.name as name, s.environmentCrn as environmentCrn " +
            "FROM Stack s WHERE s.accountId = :accountId AND s.terminated = -1")
    List<ResourceBasicView> findAllResourceBasicViewByAccountId(@Param("accountId") String accountId);

    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId AND s.environmentCrn = :environmentCrn AND s.terminated = -1")
    Optional<Stack> findByEnvironmentCrnAndAccountId(@Param("environmentCrn") String environmentCrn, @Param("accountId") String accountId);

    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId AND s.environmentCrn = :environmentCrn")
    List<Stack> findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminated(@Param("environmentCrn") String environmentCrn, @Param("accountId") String accountId);

    @Query("SELECT s FROM Stack s "
            + "LEFT JOIN FETCH s.instanceGroups ig "
            + "LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.accountId = :accountId AND s.environmentCrn = :environmentCrn")
    List<Stack> findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminatedWithList(
            @Param("environmentCrn") String environmentCrn,
            @Param("accountId") String accountId);

    @Query("SELECT s.resourceCrn as resourceCrn, s.id as id, s.name as name, s.environmentCrn as environmentCrn "
            + "FROM Stack s LEFT JOIN ChildEnvironment c ON c.stack.id = s.id WHERE s.accountId = :accountId "
            + "AND (s.environmentCrn IN :environmentCrns OR c.environmentCrn IN :environmentCrns) AND s.terminated = -1")
    List<ResourceBasicView> findMultipleResourceBasicViewByEnvironmentCrnOrChildEnvironmentCrnAndAccountId(
            @Param("environmentCrns") Collection<String> environmentCrns,
            @Param("accountId") String accountId);

    @Query("SELECT new com.sequenceiq.freeipa.entity.projection.StackUserSyncView(s.id, s.resourceCrn, s.name, s.environmentCrn, s.accountId, s.cloudPlatform, "
            + "ss.status) "
            + "FROM Stack s "
            + "LEFT JOIN ChildEnvironment c ON c.stack.id = s.id "
            + "LEFT JOIN StackStatus ss ON s.stackStatus.id = ss.id "
            + "WHERE s.accountId = :accountId "
            + "AND (s.environmentCrn IN :environmentCrns OR c.environmentCrn IN :environmentCrns) "
            + "AND s.terminated = -1 "
            + "AND ss.detailedStackStatus IN :statuses")
    List<StackUserSyncView> findAllUserSyncViewByEnvironmentCrnOrChildEnvironmentCrnAndAccountId(@Param("environmentCrns") Collection<String> environmentCrns,
            @Param("accountId") String accountId, @Param("statuses") Collection<DetailedStackStatus> statuses);

    @Query("SELECT DISTINCT s FROM Stack s "
            + "LEFT JOIN FETCH s.instanceGroups ig "
            + "LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.cloudPlatform IN :cloudPlatforms AND s.environmentCrn IN :environmentCrns AND s.terminated = -1")
    List<Stack> getByEnvironmentCrnsAndCloudPlatforms(
            @Param("environmentCrns") Collection<String> environmentCrns,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @Query("SELECT s FROM Stack s WHERE s.accountId = :accountId AND s.environmentCrn = :environmentCrn AND s.terminated = -1")
    List<Stack> findAllByEnvironmentCrnAndAccountId(@Param("environmentCrn") String environmentCrn, @Param("accountId") String accountId);

    @Query("SELECT s.resourceCrn as resourceCrn, s.id as id, s.name as name, s.environmentCrn as environmentCrn " +
            "FROM Stack s WHERE s.accountId = :accountId AND s.environmentCrn = :environmentCrn AND s.terminated = -1")
    List<ResourceBasicView> findAllResourceBasicViewByEnvironmentCrnAndAccountId(@Param("environmentCrn") String environmentCrn,
            @Param("accountId") String accountId);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig "
            + "LEFT JOIN FETCH ig.instanceMetaData WHERE s.environmentCrn = :environmentCrn AND s.accountId = :accountId AND s.terminated = -1")
    Optional<Stack> findByEnvironmentCrnAndAccountIdWithList(@Param("environmentCrn") String environmentCrn, @Param("accountId") String accountId);

    @Query("SELECT s FROM Stack s LEFT JOIN ChildEnvironment c ON c.stack.id = s.id LEFT JOIN FETCH s.instanceGroups ig "
            + "LEFT JOIN FETCH ig.instanceMetaData WHERE c.environmentCrn = :environmentCrn AND s.accountId = :accountId AND s.terminated = -1")
    Optional<Stack> findByChildEnvironmentCrnAndAccountIdWithList(@Param("environmentCrn") String environmentCrn, @Param("accountId") String accountId);

    @Query("SELECT new com.sequenceiq.freeipa.dto.StackIdWithStatus(s.id,s.stackStatus.status) FROM Stack s WHERE s.id IN (:ids)")
    List<StackIdWithStatus> findStackStatusesWithoutAuth(@Param("ids") Set<Long> ids);

    @Query("SELECT s FROM Stack s WHERE s.stackStatus.detailedStackStatus IN :detailedStackStatuses AND s.terminated = -1 ")
    List<Stack> findAllWithDetailedStackStatuses(@Param("detailedStackStatuses") Collection<DetailedStackStatus> detailedStackStatuses);

    @Query("SELECT s.id FROM Stack s WHERE s.accountId = :accountId AND s.terminated = -1")
    List<Long> findStackIdsByAccountId(@Param("accountId") String accountId);

    @Query("SELECT s.name as name, s.resourceCrn as crn FROM Stack s" +
            " WHERE s.accountId = :accountId AND s.terminated = -1 AND s.environmentCrn IN (:environmentCrns)")
    List<ResourceCrnAndNameView> findNamesByEnvironmentCrnAndAccountId(@Param("environmentCrns") Collection<String> environmentCrns,
            @Param("accountId") String accountId);

    @Query("SELECT s.name as name, s.resourceCrn as crn FROM Stack s" +
            " WHERE s.accountId = :accountId AND s.terminated = -1 AND s.resourceCrn IN (:resourceCrns)")
    List<ResourceCrnAndNameView> findNamesByResourceCrnAndAccountId(@Param("resourceCrns") Collection<String> resourceCrns,
            @Param("accountId") String accountId);

    @Query("SELECT new com.sequenceiq.cloudbreak.common.event.PayloadContext(s.resourceCrn, s.environmentCrn, s.cloudPlatform) " +
            "FROM Stack s " +
            "WHERE s.id = :id")
    Optional<PayloadContext> getStackAsPayloadContextById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Stack s SET s.ccmV2AgentCrn = :ccmV2AgentCrn WHERE s.id = :id")
    int setCcmV2AgentCrnByStackId(@Param("id") Long id, @Param("ccmV2AgentCrn") String ccmV2AgentCrn);

    @Modifying
    @Query("UPDATE Stack s SET s.tunnel = :tunnel WHERE s.id = :id")
    int setTunnelByStackId(@Param("id") Long id, @Param("tunnel") Tunnel tunnel);

    @Query("SELECT s.accountId FROM Stack s WHERE s.id = :id")
    Optional<String> findAccountIdByStackId(@Param("id") Long id);

    @Query("SELECT s.environmentCrn FROM Stack s WHERE s.id = :id")
    Optional<String> findEnvironmentCrnByStackId(@Param("id") Long id);

}
