package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.repository.snippets.ShowTerminatedClustersSnippets.SHOW_TERMINATED_CLUSTERS_IF_REQUESTED;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.authorization.service.model.projection.ResourceCrnAndNameView;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.domain.projection.StackClusterStatusView;
import com.sequenceiq.cloudbreak.domain.projection.StackCrnView;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.projection.StackImageView;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.projection.StackPlatformVariantView;
import com.sequenceiq.cloudbreak.domain.projection.StackStatusView;
import com.sequenceiq.cloudbreak.domain.projection.StackTtlView;
import com.sequenceiq.cloudbreak.domain.stack.DnsResolverType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceRepository;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.model.ProviderSyncState;

@EntityType(entityClass = Stack.class)
@Transactional(TxType.REQUIRED)
public interface StackRepository extends WorkspaceResourceRepository<Stack, Long>, JobResourceRepository<Stack, Long> {

    @Override
    @Query("SELECT s FROM Stack s WHERE s.workspace.id = :workspaceId")
    Set<Stack> findAllByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("SELECT s.id as id, s.name as name, s.resourceCrn as crn from Stack s "
            + "WHERE s.cluster.clusterManagerIp= :clusterManagerIp AND s.terminated IS null "
            + "AND (s.type <> 'TEMPLATE' OR s.type is null)")
    Optional<StackIdView> findByAmbari(@Param("clusterManagerIp") String clusterManagerIp);

    @Query("SELECT s FROM Stack s WHERE s.name= :name AND s.workspace.id= :workspaceId AND s.terminated IS null "
            + "AND (s.type <> 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findByNameAndWorkspaceId(@Param("name") String name, @Param("workspaceId") Long workspaceId);

    @Query("SELECT s FROM Stack s WHERE s.resourceCrn= :crn AND s.workspace.id= :workspaceId AND s.terminated IS null "
            + "AND (s.type <> 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findNotTerminatedByCrnAndWorkspaceId(@Param("crn") String crn, @Param("workspaceId") Long workspaceId);

    @Query("SELECT s FROM Stack s WHERE s.resourceCrn= :crn AND s.workspace.id= :workspaceId "
            + "AND (s.type <> 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findByCrnAndWorkspaceId(@Param("crn") String crn, @Param("workspaceId") Long workspaceId);

    @Query("SELECT s FROM Stack s WHERE s.name in :names AND s.workspace.id= :workspaceId AND s.terminated IS null "
            + "AND (s.type <> 'TEMPLATE' OR s.type is null)")
    Set<Stack> findByNameInAndWorkspaceId(@Param("names") Set<String> name, @Param("workspaceId") Long workspaceId);

    @Query("SELECT s FROM Stack s WHERE s.name= :name AND s.workspace.id= :workspaceId AND s.type = :type AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED)
    Optional<Stack> findByNameAndWorkspaceIdWithLists(@Param("name") String name, @Param("type") StackType type, @Param("workspaceId") Long workspaceId,
            @Param("showTerminated") Boolean showTerminated, @Param("terminatedAfter") Long terminatedAfter);

    @Query("SELECT s.id as id, s.name as name, s.resourceCrn as crn FROM Stack s "
            + "WHERE s.environmentCrn = :environmentCrn AND s.type = :type AND s.terminated IS null")
    List<StackIdView> findByEnvironmentCrnAndStackType(@Param("environmentCrn") String environmentCrn, @Param("type") StackType type);

    @Query("SELECT COUNT(s.id) FROM Stack s "
            + "WHERE s.environmentCrn= :environmentCrn AND s.type = :type AND s.terminated IS null")
    Long countByEnvironmentCrnAndStackType(@Param("environmentCrn") String environmentCrn, @Param("type") StackType type);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData LEFT JOIN FETCH ig.template "
            + "LEFT JOIN FETCH ig.securityGroup LEFT JOIN FETCH ig.instanceGroupNetwork WHERE s.name= :name " +
            "AND s.workspace.id= :workspaceId AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED)
    Optional<Stack> findByNameAndWorkspaceIdWithLists(@Param("name") String name, @Param("workspaceId") Long workspaceId,
            @Param("showTerminated") Boolean showTerminated,
            @Param("terminatedAfter") Long terminatedAfter);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData LEFT JOIN FETCH ig.template "
            + "LEFT JOIN FETCH ig.securityGroup LEFT JOIN FETCH ig.instanceGroupNetwork WHERE s.resourceCrn= :crn " +
            "AND s.workspace.id= :workspaceId AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED)
    Optional<Stack> findByCrnAndWorkspaceIdWithLists(@Param("crn") String crn, @Param("workspaceId") Long workspaceId,
            @Param("showTerminated") Boolean showTerminated, @Param("terminatedAfter") Long terminatedAfter);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData LEFT JOIN FETCH ig.template "
            + "LEFT JOIN FETCH ig.securityGroup LEFT JOIN FETCH ig.instanceGroupNetwork WHERE s.resourceCrn= :crn " +
            "AND s.workspace.id= :workspaceId AND s.type = :type AND " +
            SHOW_TERMINATED_CLUSTERS_IF_REQUESTED)
    Optional<Stack> findByCrnAndWorkspaceIdWithLists(@Param("crn") String crn, @Param("type") StackType type, @Param("workspaceId") Long workspaceId,
            @Param("showTerminated") Boolean showTerminated, @Param("terminatedAfter") Long terminatedAfter);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData LEFT JOIN FETCH ig.template " +
            "LEFT JOIN FETCH ig.securityGroup LEFT JOIN FETCH ig.instanceGroupNetwork LEFT JOIN FETCH ig.availabilityZones " +
            "WHERE s.id= :id AND (s.type <> 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findOneWithLists(@Param("id") Long id);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData im LEFT JOIN FETCH ig.template " +
            "WHERE s.id= :id AND ig.instanceGroupType = 'GATEWAY' AND im.instanceMetadataType = 'GATEWAY_PRIMARY' " +
            "AND (s.type <> 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findOneWithGateway(@Param("id") Long id);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData LEFT JOIN FETCH ig.template " +
            "LEFT JOIN FETCH ig.securityGroup LEFT JOIN FETCH ig.instanceGroupNetwork WHERE s.resourceCrn= :crn " +
            "AND (s.type <> 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findOneByCrnWithLists(@Param("crn") String crn);

    @Query("SELECT s FROM Stack s "
            + "LEFT JOIN FETCH s.instanceGroups ig "
            + "LEFT JOIN FETCH ig.instanceMetaData "
            + "LEFT JOIN FETCH ig.template "
            + "LEFT JOIN FETCH ig.securityGroup "
            + "LEFT JOIN FETCH ig.instanceGroupNetwork "
            + "LEFT JOIN FETCH s.cluster c "
            + "LEFT JOIN FETCH c.components "
            + "WHERE s.id= :id "
            + "AND (s.type <> 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findOneWithCluster(@Param("id") Long id);

    @Query("SELECT s.id as id, s.name as name, s.stackStatus as status FROM Stack s WHERE s.id IN (:ids) AND (s.type <> 'TEMPLATE' OR s.type is null)")
    List<StackStatusView> findStackStatusesWithoutAuth(@Param("ids") Set<Long> ids);

    @Query("SELECT s.id as id, s.name as name, s.resourceCrn as crn, s.workspace as workspace, "
            + "s.stackStatus as status, c.creationFinished as creationFinished "
            + "FROM Stack s LEFT JOIN s.cluster c LEFT JOIN s.workspace WHERE s.terminated IS null " +
            "AND (s.type <> 'TEMPLATE' OR s.type is null)")
    List<StackTtlView> findAllAlive();

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig "
            + "WHERE s.terminated IS null AND (s.type <> 'TEMPLATE' OR s.type is null)")
    Set<Stack> findAllAliveWithInstanceGroups();

    @Query("SELECT DISTINCT s.id as id, im.image as image FROM Stack s JOIN s.instanceGroups ig JOIN ig.instanceMetaData im "
            + "WHERE (s.terminated IS null OR s.terminated >= :thresholdTimestamp) " +
            "AND (s.type <> 'TEMPLATE' OR s.type is null) AND im.image is not null")
    List<StackImageView> findImagesOfAliveStacks(@Param("thresholdTimestamp") long thresholdTimestamp);

    @Query("SELECT s.id as id, s.name as name, s.stackStatus as status FROM Stack s "
            + "WHERE s.stackStatus.status IN :statuses AND (s.type <> 'TEMPLATE' OR s.type is null)")
    List<StackStatusView> findByStatuses(@Param("statuses") List<Status> statuses);

    @Query("SELECT s.id as id, "
            + "s.resourceCrn as crn, "
            + "s.name as name, "
            + "ss.status as status, "
            + "ss.statusReason as statusReason, "
            + "c.id as clusterId, "
            + "c.status as clusterStatus, "
            + "c.statusReason as clusterStatusReason, "
            + "c.certExpirationState as certExpirationState, "
            + "c.certExpirationDetails as certExpirationDetails, "
            + "s.providerSyncStates as providerSyncStates "
            + "FROM Stack s "
            + "LEFT JOIN s.cluster c "
            + "LEFT JOIN s.stackStatus ss "
            + "WHERE s.resourceCrn = :crn")
    Optional<StackClusterStatusView> getStatusByCrn(@Param("crn") String crn);

    @Query("SELECT s.id as id, "
            + "s.resourceCrn as crn, "
            + "s.name as name, "
            + "ss.status as status, "
            + "ss.statusReason as statusReason, "
            + "c.id as clusterId, "
            + "c.status as clusterStatus, "
            + "c.statusReason as clusterStatusReason, "
            + "c.certExpirationState as certExpirationState "
            + "FROM Stack s "
            + "LEFT JOIN s.cluster c "
            + "LEFT JOIN s.stackStatus ss "
            + "WHERE s.id IN :ids")
    List<StackClusterStatusView> findStatusesByIds(@Param("ids") List<Long> ids);

    @Query("SELECT s.id as id, "
            + "s.resourceCrn as crn, "
            + "s.name as name, "
            + "ss.status as status, "
            + "ss.statusReason as statusReason, "
            + "c.id as clusterId, "
            + "c.status as clusterStatus, "
            + "c.statusReason as clusterStatusReason, "
            + "c.certExpirationState as certExpirationState "
            + "FROM Stack s "
            + "LEFT JOIN s.cluster c "
            + "LEFT JOIN s.stackStatus ss "
            + "WHERE s.terminated > :since")
    List<StackClusterStatusView> getDeletedStacks(@Param("since") Long since);

    @Query("SELECT s.id as id, "
            + "s.resourceCrn as crn, "
            + "s.name as name, "
            + "ss.status as status, "
            + "ss.statusReason as statusReason, "
            + "c.id as clusterId, "
            + "c.status as clusterStatus, "
            + "c.statusReason as clusterStatusReason, "
            + "c.certExpirationState as certExpirationState "
            + "FROM Stack s "
            + "LEFT JOIN s.cluster c "
            + "LEFT JOIN s.stackStatus ss "
            + "WHERE s.resourceCrn = :crn AND s.workspace.id= :workspaceId")
    Optional<StackClusterStatusView> getStatusByCrnAndWorkspace(@Param("crn") String crn, @Param("workspaceId") Long workspaceId);

    @Query("SELECT s.id as id, "
            + "s.resourceCrn as crn, "
            + "s.name as name, "
            + "ss.status as status, "
            + "ss.statusReason as statusReason, "
            + "c.id as clusterId, "
            + "c.status as clusterStatus, "
            + "c.statusReason as clusterStatusReason, "
            + "c.certExpirationState as certExpirationState "
            + "FROM Stack s "
            + "LEFT JOIN s.cluster c "
            + "LEFT JOIN s.stackStatus ss "
            + "WHERE s.resourceCrn in :crns AND (s.type IS null OR s.type = :stackType)")
    List<StackClusterStatusView> getStatusByCrnsInternal(@Param("crns") List<String> crns, @Param("stackType") StackType stackType);

    @Query("SELECT s.id as id, "
            + "s.resourceCrn as crn, "
            + "s.name as name, "
            + "ss.status as status, "
            + "ss.statusReason as statusReason, "
            + "c.id as clusterId, "
            + "c.status as clusterStatus, "
            + "c.statusReason as clusterStatusReason, "
            + "c.certExpirationState as certExpirationState "
            + "FROM Stack s "
            + "LEFT JOIN s.cluster c "
            + "LEFT JOIN s.stackStatus ss "
            + "WHERE s.name = :name AND s.workspace.id= :workspaceId")
    Optional<StackClusterStatusView> getStatusByNameAndWorkspace(@Param("name") String name, @Param("workspaceId") Long workspaceId);

    @Query("SELECT s.id as id, "
            + "s.name as name, "
            + "s.gatewayPort as gatewayPort, "
            + "s.created as created, "
            + "ss.status as stackStatus, "
            + "c.cloudbreakClusterManagerUser as cloudbreakClusterManagerUser, "
            + "c.cloudbreakClusterManagerPassword as cloudbreakClusterManagerPassword, "
            + "c.status as clusterStatus, "
            + "ig.instanceGroupType, "
            + "im.instanceMetadataType, "
            + "im.publicIp as publicIp, "
            + "s.multiAz as multiAz, "
            + "im.privateIp as privateIp, "
            + "sc.usePrivateIpToTls as usePrivateIpToTls, "
            + "w.id as workspaceId, "
            + "t.name as tenantName, "
            + "u.userId as userId, "
            + "u.userCrn as userCrn, "
            + "s.resourceCrn as crn, "
            + "c.variant as clusterManagerVariant, "
            + "s.tunnel as tunnel, "
            + "s.cloudPlatform as cloudPlatform, "
            + "s.type as type "
            + "FROM Stack s "
            + "LEFT JOIN s.cluster c "
            + "LEFT JOIN s.stackStatus ss "
            + "LEFT JOIN s.instanceGroups ig "
            + "LEFT JOIN ig.instanceMetaData im "
            + "LEFT JOIN s.securityConfig sc "
            + "LEFT JOIN s.workspace w "
            + "LEFT JOIN w.tenant t "
            + "LEFT JOIN s.creator u "
            + "WHERE ig.instanceGroupType = 'GATEWAY' "
            + "AND im.instanceMetadataType = 'GATEWAY_PRIMARY' "
            + "AND s.terminated IS null "
            + "AND c.status in ('AVAILABLE', 'REQUESTED', 'UPDATE_IN_PROGRESS') "
            + "AND (s.type <> 'TEMPLATE' OR s.type is null)")
    Set<AutoscaleStack> findAliveOnesWithClusterManager();

    @Query("SELECT s.id as id, s.name as name FROM Stack s WHERE s.network = :network")
    Set<StackIdView> findByNetwork(@Param("network") Network network);

    @Query("SELECT s.workspace.id FROM Stack s where s.resourceCrn = :crn")
    Long findWorkspaceIdByCrn(@Param("crn") String crn);

    @Query("SELECT s.workspace FROM Stack s where s.resourceCrn = :crn")
    Optional<Workspace> findWorkspaceByCrn(@Param("crn") String crn);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE s.id = :id "
            + "AND s.type = 'TEMPLATE'")
    Optional<Stack> findTemplateWithLists(@Param("id") Long id);

    @Query("SELECT s FROM Stack s WHERE s.resourceCrn = :crn")
    Optional<Stack> findByResourceCrn(@Param("crn") String crn);

    @Query(value = "SELECT environmentcrn FROM stack WHERE resourcecrn = :crn", nativeQuery = true)
    Optional<String> findEnvCrnByResourceCrn(@Param("crn") String crn);

    @Query("SELECT s.resourceCrn as resourceCrn, s.environmentCrn as environmentCrn FROM Stack s WHERE s.resourceCrn IN (:crns)")
    List<StackCrnView> findAllByResourceCrn(@Param("crns") Set<String> crn);

    @Query("SELECT s.id as id, s.name as name, s.resourceCrn as crn FROM Stack s WHERE s.datalakeCrn  = :crn")
    Set<StackIdView> findByDatalakeCrn(@Param("crn") String crn);

    @Query("SELECT s.id as id, s.name as name, s.resourceCrn as crn FROM Stack s WHERE s.datalakeCrn = :crn AND s.terminated IS NULL")
    Set<StackIdView> findNotTerminatedByDatalakeCrn(@Param("crn") String crn);

    @Query("SELECT VALUE(s.parameters) FROM Stack s WHERE s.id = :stackId AND KEY(s.parameters) = :ttlKey")
    String findTimeToLiveValueForSTack(@Param("stackId") Long stackId, @Param("ttlKey") String ttl);

    @Query("SELECT new java.lang.Boolean(count(*) > 0) FROM Stack s WHERE s.terminated IS null AND s.workspace.id= :workspaceId")
    Boolean anyStackInWorkspace(@Param("workspaceId") Long workspaceId);

    @Query("SELECT new java.lang.Boolean(count(*) > 0) "
            + "FROM Stack c LEFT JOIN c.instanceGroups ig WHERE ig.template.id = :templateId AND c.stackStatus.status <> 'DELETE_COMPLETED'")
    Boolean findTemplateInUse(@Param("templateId") Long templateId);

    @Query("SELECT s.id FROM Stack s WHERE s.name= :name AND s.workspace.id = :workspaceId AND s.terminated IS null")
    Optional<Long> findIdByNameAndWorkspaceId(@Param("name") String name, @Param("workspaceId") Long workspaceId);

    @Query("SELECT s.id FROM Stack s WHERE s.resourceCrn= :crn AND s.workspace.id = :workspaceId AND s.terminated IS null")
    Optional<Long> findIdByCrnAndWorkspaceId(@Param("crn") String crn, @Param("workspaceId") Long workspaceId);

    @Query("SELECT s.resourceCrn FROM Stack s WHERE s.id = :id AND s.terminated IS null")
    Optional<String> findCrnById(@Param("id") Long id);

    @Query("SELECT s.name as name, s.resourceCrn as crn FROM Stack s WHERE s.workspace.id = :workspaceId AND s.resourceCrn IN (:resourceCrns)")
    List<ResourceCrnAndNameView> findResourceNamesByCrnAndWorkspaceId(@Param("resourceCrns") Collection<String> resourceCrns,
            @Param("workspaceId") Long workspaceId);

    @Query("SELECT s.id as id, "
            + "s.resourceCrn as resourceCrn, "
            + "s.name as name, "
            + "s.tunnel as tunnel, "
            + "s.multiAz as multiAz, "
            + "s.environmentCrn as environmentCrn, "
            + "s.type as type, "
            + "b.resourceCrn as blueprintCrn, "
            + "b.name as blueprintName, "
            + "b.created as blueprintCreated, "
            + "b.stackType as stackType, "
            + "CASE WHEN (s.stackVersion is not null) THEN s.stackVersion ELSE b.stackVersion END as stackVersion,"
            + "ss.status as stackStatus, "
            + "s.cloudPlatform as cloudPlatform, "
            + "c.status as clusterStatus, "
            + "s.created as created, "
            + "s.datalakeCrn as datalakeCrn, "
            + "b.tags as blueprintTags,"
            + "c.id as clusterId, "
            + "s.platformVariant as platformVariant, "
            + "c.clusterManagerIp as clusterManagerIp, "
            + "b.id as blueprintId, "
            + "b.hostGroupCount as hostGroupCount, "
            + "b.status as blueprintStatus, "
            + "b.blueprintUpgradeOption as blueprintUpgradeOption, "
            + "s.terminated as terminated, "
            + "u.id as userDOId, "
            + "u.userId as userId, "
            + "u.userName as username, "
            + "u.userCrn as usercrn, "
            + "c.certExpirationState as certExpirationState, "
            + "s.providerSyncStates as providerSyncStates, "
            + "db.externalDatabaseAvailabilityType as externalDatabaseCreationType, "
            + "db.externalDatabaseEngineVersion as externalDatabaseEngineVersion "
            + "FROM Stack s "
            + "LEFT JOIN s.cluster c "
            + "LEFT JOIN c.blueprint b "
            + "LEFT JOIN s.stackStatus ss "
            + "LEFT JOIN s.creator u "
            + "LEFT JOIN s.database db "
            + "WHERE s.workspace.id = :id "
            + "AND s.terminated IS null "
            + "AND (:environmentCrn IS null OR s.environmentCrn = :environmentCrn) "
            + "AND (s.type IS null OR s.type in :stackTypes)")
    Set<StackListItem> findByWorkspaceId(@Param("id") Long id, @Param("environmentCrn") String environmentCrn, @Param("stackTypes") List<StackType> stackTypes);

    @Query("SELECT s.id as id, "
            + "s.resourceCrn as resourceCrn, "
            + "s.name as name, "
            + "s.tunnel as tunnel, "
            + "s.multiAz as multiAz, "
            + "s.environmentCrn as environmentCrn, "
            + "s.type as type, "
            + "b.resourceCrn as blueprintCrn, "
            + "b.name as blueprintName, "
            + "b.created as blueprintCreated, "
            + "b.stackType as stackType, "
            + "CASE WHEN (s.stackVersion is not null) THEN s.stackVersion ELSE b.stackVersion END as stackVersion,"
            + "ss.status as stackStatus, "
            + "s.cloudPlatform as cloudPlatform, "
            + "c.status as clusterStatus, "
            + "s.created as created, "
            + "s.datalakeCrn as datalakeCrn, "
            + "b.tags as blueprintTags,"
            + "c.id as clusterId, "
            + "s.platformVariant as platformVariant, "
            + "c.clusterManagerIp as clusterManagerIp, "
            + "b.id as blueprintId, "
            + "b.hostGroupCount as hostGroupCount, "
            + "b.status as blueprintStatus, "
            + "b.blueprintUpgradeOption as blueprintUpgradeOption, "
            + "s.terminated as terminated, "
            + "u.id as userDOId, "
            + "u.userId as userId, "
            + "u.userName as username, "
            + "u.userCrn as usercrn, "
            + "c.certExpirationState as certExpirationState, "
            + "db.externalDatabaseAvailabilityType as externalDatabaseCreationType, "
            + "db.externalDatabaseEngineVersion as externalDatabaseEngineVersion "
            + "FROM Stack s "
            + "LEFT JOIN s.cluster c "
            + "LEFT JOIN c.blueprint b "
            + "LEFT JOIN s.stackStatus ss "
            + "LEFT JOIN s.creator u "
            + "LEFT JOIN s.database db "
            + "WHERE s.id in :stackIds "
            + "AND s.workspace.id = :workspaceId AND s.terminated IS null "
            + "AND (s.type IS null OR s.type in :stackTypes)")
    Set<StackListItem> findByWorkspaceIdAnStackIds(@Param("workspaceId") Long workspaceId, @Param("stackIds") List<Long> stackIds,
            @Param("stackTypes") List<StackType> stackTypes);

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(s.id, s.resourceCrn, s.environmentCrn) "
            + "FROM Stack s "
            + "WHERE s.workspace.id = :id AND s.terminated IS null "
            + "AND s.environmentCrn = :environmentCrn "
            + "AND (s.type IS null OR s.type IN :stackTypes)")
    List<ResourceWithId> getAsAuthorizationResourcesByEnvCrn(@Param("id") Long id, @Param("environmentCrn") String environmentCrn,
            @Param("stackTypes") List<StackType> stackTypes);

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(s.id, s.resourceCrn, s.environmentCrn) "
            + "FROM Stack s "
            + "WHERE s.workspace.id = :id AND s.terminated IS null "
            + "AND (s.type IS null OR s.type IN :stackTypes)")
    List<ResourceWithId> getAsAuthorizationResources(@Param("id") Long id, @Param("stackTypes") List<StackType> stackTypes);

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(s.id, s.resourceCrn, s.environmentCrn) "
            + "FROM Stack s "
            + "WHERE s.workspace.id = :id "
            + "AND s.resourceCrn IN :crns "
            + "AND s.terminated IS null "
            + "AND (s.type IS null OR s.type = :stackType)")
    List<ResourceWithId> getAsAuthorizationResourcesByCrns(@Param("id") Long id, @Param("stackType") StackType stackType,
            @Param("crns") List<String> crns);

    @Modifying
    @Query("UPDATE Stack s SET s.minaSshdServiceId = :minaSshdServiceId WHERE s.id = :id")
    int setMinaSshdServiceIdByStackId(@Param("id") Long id, @Param("minaSshdServiceId") String minaSshdServiceId);

    @Modifying
    @Query("UPDATE Stack s SET s.ccmV2AgentCrn = :ccmV2AgentCrn WHERE s.id = :id")
    int setCcmV2AgentCrnByStackId(@Param("id") Long id, @Param("ccmV2AgentCrn") String ccmV2AgentCrn);

    @Modifying
    @Query("UPDATE Stack s SET s.multiAz = :multiAz WHERE s.id = :id")
    void setMultiAzFlag(@Param("id") Long id, @Param("multiAz") boolean multiAz);

    StackPlatformVariantView findPlatformVariantAndCloudPlatformById(Long id);

    @Query("SELECT new com.sequenceiq.cloudbreak.common.event.PayloadContext(s.resourceCrn, s.cloudPlatform) " +
            "FROM Stack s " +
            "WHERE s.id = :id")
    Optional<PayloadContext> findStackAsPayloadContext(@Param("id") Long id);

    @Query("SELECT s.id as localId, s.resourceCrn as remoteResourceId, s.name as name, s.cloudPlatform as provider " +
            "FROM Stack s " +
            "WHERE s.id = :resourceId")
    Optional<JobResource> getJobResource(@Param("resourceId") Long resourceId);

    @Query("SELECT s.id as localId, s.resourceCrn as remoteResourceId, s.name as name, s.cloudPlatform as provider " +
            "FROM Stack s " +
            "LEFT JOIN s.stackStatus ss " +
            "WHERE s.terminated IS null " +
            "AND (s.type <> 'TEMPLATE' OR s.type is null) " +
            "AND ss.status not in (:notInStatuses)")
    List<JobResource> getJobResourcesNotIn(@Param("notInStatuses") Set<Status> notInStatuses);

    @Query("SELECT s.id as localId, s.resourceCrn as remoteResourceId, s.name as name, s.cloudPlatform as provider " +
            "FROM Stack s " +
            "LEFT JOIN s.stackStatus ss " +
            "WHERE s.terminated IS null " +
            "AND s.type = 'WORKLOAD' " +
            "AND ss.status not in (:notInStatuses)")
    List<JobResource> getDatahubJobResourcesNotIn(@Param("notInStatuses") Set<Status> notInStatuses);

    @Query("SELECT s.id as id, s.resourceCrn as resourceCrn, s.name as name " +
            "FROM Stack s " +
            "WHERE s.terminated IS null " +
            "AND (s.type <> 'TEMPLATE' OR s.type is null) " +
            "AND s.resourceCrn = :resourceCrn")
    Optional<ResourceBasicView> findResourceBasicViewByResourceCrn(@Param("resourceCrn") String resourceCrn);

    @Query("SELECT s.id as id, s.resourceCrn as resourceCrn, s.name as name " +
            "FROM Stack s " +
            "WHERE s.terminated IS null " +
            "AND (s.type <> 'TEMPLATE' OR s.type is null) " +
            "AND s.resourceCrn in (:resourceCrns)")
    List<ResourceBasicView> findAllResourceBasicViewByResourceCrns(@Param("resourceCrns") Collection<String> resourceCrns);

    @Query("SELECT s.id as id, s.resourceCrn as resourceCrn, s.name as name " +
            "FROM Stack s " +
            "LEFT JOIN s.workspace w " +
            "WHERE s.terminated IS null " +
            "AND (s.type <> 'TEMPLATE' OR s.type is null) " +
            "AND s.name = :name " +
            "AND w.id = :workspaceId")
    Optional<ResourceBasicView> findResourceBasicViewByNameAndWorkspaceId(@Param("name") String name, @Param("workspaceId") Long workspaceId);

    @Query("SELECT s.id as id, s.resourceCrn as resourceCrn, s.name as name " +
            "FROM Stack s " +
            "LEFT JOIN s.workspace w " +
            "WHERE s.terminated IS null " +
            "AND (s.type <> 'TEMPLATE' OR s.type is null) " +
            "AND s.name in (:names) " +
            "AND w.id = :workspaceId")
    List<ResourceBasicView> findAllResourceBasicViewByNamesAndWorkspaceId(@Param("names") Collection<String> names, @Param("workspaceId") Long workspaceId);

    @Modifying
    @Query("UPDATE Stack s SET s.domainDnsResolver = :domainDnsResolver WHERE s.id = :stackId")
    int updateDomainDnsResolverByStackId(@Param("stackId") Long stackId, @Param("domainDnsResolver") DnsResolverType domainDnsResolver);

    @Modifying
    @Query("UPDATE Stack s SET s.tunnel = :tunnel WHERE s.id = :id")
    int setTunnelByStackId(@Param("id") Long id, @Param("tunnel") Tunnel tunnel);

    @Query("SELECT COUNT(*) " +
            "FROM Stack " +
            "WHERE environmentCrn = :envCrn " +
            "AND terminated IS NULL " +
            "AND type IN ('DATALAKE', 'WORKLOAD') " +
            "AND tunnel IN (:upgradableTunnels)")
    int getNotUpgradedStackCount(@Param("envCrn") String envCrn, @Param("upgradableTunnels") Collection<Tunnel> upgradableTunnels);

    @Modifying
    @Query("UPDATE Stack s SET s.securityConfig = :securityConfig WHERE s.id = :stackId")
    void updateSecurityConfigByStackId(@Param("stackId") Long stackId, @Param("securityConfig") SecurityConfig securityConfig);

    @Modifying
    @Query("UPDATE Stack s SET s.customDomain = :customDomain WHERE s.id = :stackId")
    void updateCustomDomainByStackId(@Param("stackId") Long stackId, @Param("customDomain") String customDomain);

    @Modifying
    @Query("UPDATE Stack s SET s.stackVersion = :stackVersion WHERE s.id = :stackId")
    void updateStackVersion(@Param("stackId") Long stackId, @Param("stackVersion") String stackVersion);

    @Modifying
    @Query("UPDATE Stack s SET s.javaVersion = :javaVersion WHERE s.id = :stackId")
    void updateJavaVersion(@Param("stackId") Long stackId, @Param("javaVersion") String javaVersion);

    @Modifying
    @Query("UPDATE Stack s SET s.providerSyncStates = :providerSyncStates WHERE s.id = :stackId")
    void updateProviderSyncStates(@Param("stackId") Long stackId, @Param("providerSyncStates") Set<ProviderSyncState> providerSyncStates);

    @Query("SELECT s.providerSyncStates FROM Stack s WHERE s.id = :stackId")
    String getProviderSyncStatesByStackId(@Param("stackId") Long stackId);

    @Query("SELECT s.environmentCrn FROM Stack s WHERE s.id = :stackId")
    Optional<String> findEnvironmentCrnByStackId(@Param("stackId") Long stackId);

    @Query("SELECT s.region FROM Stack s WHERE s.id = :stackId")
    Optional<String> findRegionByStackId(@Param("stackId") Long stackId);

    @Query("SELECT s.database.id FROM Stack s WHERE s.id = :stackId")
    Optional<Long> findDatabaseIdByStackId(@Param("stackId") Long stackId);

    @Query("SELECT s.resourceCrn FROM Stack s WHERE s.terminated < :date")
    Page<String> findAllTerminatedBefore(Long date, Pageable pageable);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups "
            + "WHERE s.resourceCrn = :resourceCrn")
    Optional<Stack> findByResourceCrnArchivedIsTrue(@Param("resourceCrn") String resourceCrn);

    @Modifying
    void deleteByResourceCrn(String crn);

}

