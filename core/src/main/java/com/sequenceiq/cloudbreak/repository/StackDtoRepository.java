package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.repository.snippets.ShowTerminatedClustersSnippets.SHOW_TERMINATED_CLUSTERS_IF_REQUESTED;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.view.delegate.StackViewDelegate;

@org.springframework.stereotype.Repository
public interface StackDtoRepository extends Repository<Stack, Long> {

    String BASE_QUERY = "SELECT "
            + "s.id as id, "
            + "s.resourceCrn as resourceCrn, "
            + "s.name as name, "
            + "s.region as region, "
            + "s.gatewayPort as gatewayPort, "
            + "s.tunnel as tunnel, "
            + "s.multiAz as multiAz, "
            + "s.environmentCrn as environmentCrn, "
            + "s.type as type, "
            + "CASE WHEN (s.stackVersion is not null) THEN s.stackVersion ELSE b.stackVersion END as stackVersion,"
            + "ss.status as status, "
            + "ss.detailedStackStatus as detailedStatus, "
            + "ss.statusReason as statusReason, "
            + "ss.created as statusCreated, "
            + "s.cloudPlatform as cloudPlatform, "
            + "s.created as created, "
            + "s.datalakeCrn as datalakeCrn, "
            + "s.tags as tags, "
            + "c.id as clusterId, "
            + "s.platformVariant as platformVariant, "
            + "s.customDomain as customDomain, "
            + "s.customHostname as customHostname, "
            + "s.hostgroupNameAsHostname as hostgroupNameAsHostname, "
            + "s.clusterNameAsSubdomain as clusterNameAsSubdomain, "
            + "s.displayName as displayName, "
            + "s.description as description, "
            + "s.uuid as uuid, "
            + "s.availabilityZone as availabilityZone, "
            + "s.inputs as inputs, "
            + "s.clusterProxyRegistered as clusterProxyRegistered, "
            + "s.domainDnsResolver as domainDnsResolver, "
            + "s.minaSshdServiceId as minaSshdServiceId, "
            + "s.ccmV2AgentCrn as ccmV2AgentCrn, "
            + "fp as failurePolicy, "
            + "s.onFailureActionAction as onFailureActionAction, "
            + "s.originalName as originalName, "
            + "s.javaVersion as javaVersion, "
            + "a as stackAuthentication, "
            + "w.id as workspaceId, "
            + "w.name as workspaceName, "
            + "t.id as tenantId, "
            + "t.name as tenantName, "
            + "u as creator, "
            + "s.database.id as databaseId, "
            + "s.supportedImdsVersion as supportedImdsVersion, "
            + "s.architecture as architecture, "
            + "s.providerSyncStates as providerSyncStates "
            + "FROM Stack s "
            + "LEFT JOIN s.cluster c "
            + "LEFT JOIN c.blueprint b "
            + "LEFT JOIN s.stackStatus ss "
            + "LEFT JOIN s.creator u "
            + "LEFT JOIN s.workspace w "
            + "LEFT JOIN w.tenant t "
            + "LEFT JOIN s.stackAuthentication a "
            + "LEFT JOIN s.failurePolicy fp ";

    @Query(BASE_QUERY
            + "WHERE t.name = :accountId "
            + "AND s.name = :name "
            + "AND (s.type IS null OR s.type = :stackType) "
            + "AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED
    )
    Optional<StackViewDelegate> findByName(@Param("accountId") String accountId, @Param("name") String name, @Param("stackType") StackType stackType,
            @Param("showTerminated") Boolean showTerminated, @Param("terminatedAfter") Long terminatedAfter);

    @Query(BASE_QUERY
            + "WHERE t.name = :accountId "
            + "AND s.name = :name "
            + "AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED
    )
    Optional<StackViewDelegate> findByName(@Param("accountId") String accountId, @Param("name") String name,
            @Param("showTerminated") Boolean showTerminated, @Param("terminatedAfter") Long terminatedAfter);

    @Query(BASE_QUERY
            + "WHERE t.name = :accountId "
            + "AND s.name = :name "
    )
    Optional<StackViewDelegate> findByName(@Param("accountId") String accountId, @Param("name") String name);

    @Query(BASE_QUERY
            + "WHERE s.resourceCrn = :crn "
            + "AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED
    )
    Optional<StackViewDelegate> findByCrn(@Param("crn") String crn,
            @Param("showTerminated") Boolean showTerminated, @Param("terminatedAfter") Long terminatedAfter);

    @Query(BASE_QUERY
            + "WHERE s.resourceCrn = :crn "
            + "AND (s.type IS null OR s.type = :stackType) "
            + "AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED
    )
    Optional<StackViewDelegate> findByCrn(@Param("crn") String crn, @Param("stackType") StackType stackType,
            @Param("showTerminated") Boolean showTerminated, @Param("terminatedAfter") Long terminatedAfter);

    @Query(BASE_QUERY
            + "WHERE s.resourceCrn = :crn "
    )
    Optional<StackViewDelegate> findByCrn(@Param("crn") String crn);

    @Query("SELECT n FROM Stack s " +
            "LEFT JOIN s.network n " +
            "WHERE s.id = :stackId")
    Optional<Network> getNetworkByStackById(@Param("stackId") long stackId);

    @Query("SELECT d FROM Stack s " +
            "LEFT JOIN s.database d " +
            "WHERE s.id = :stackId")
    Optional<Database> getDatabaseByStackById(@Param("stackId") long stackId);

    @Query(BASE_QUERY
            + "WHERE s.id = :stackId "
    )
    Optional<StackViewDelegate> findById(@Param("stackId") long stackId);

    @Query(BASE_QUERY
            + "WHERE s.resourceCrn in :resourceCrns "
    )
    List<StackViewDelegate> findAllByResourceCrnIn(@Param("resourceCrns") Collection<String> resourceCrns);

    @Query(BASE_QUERY
            + "WHERE s.environmentCrn in :environmentCrns "
            + "AND s.cloudPlatform in :cloudPlatforms "
            + "AND s.terminated IS null"
    )
    List<StackViewDelegate> findNotTerminatedByEnvironmentCrnsAndCloudPlatforms(
            @Param("environmentCrns") Collection<String> environmentCrns,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @Query(BASE_QUERY
            + "WHERE s.resourceCrn in :resourceCrns "
            + "AND s.cloudPlatform in :cloudPlatforms "
            + "AND s.terminated IS null"
    )
    List<StackViewDelegate> findNotTerminatedByResourceCrnsAndCloudPlatforms(
            @Param("resourceCrns") Collection<String> resourceCrns,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @Query(BASE_QUERY
            + "WHERE s.resourceCrn = :resourceCrn "
    )
    Optional<StackViewDelegate> findByResourceCrn(@Param("resourceCrn") String resourceCrn);

    @Query(BASE_QUERY
            + "WHERE s.name in :names "
            + "AND t.name = :accountId"
    )
    List<StackViewDelegate> findAllByNamesAndAccountId(@Param("names") List<String> resourceNames, @Param("accountId") String accountId);

    @Query(BASE_QUERY
            + "WHERE s.name = :names "
            + "AND t.name = :accountId"
    )
    Optional<StackViewDelegate> findByNameAndAccountId(@Param("names") String resourceName, @Param("accountId") String accountId);

    @Query(BASE_QUERY
            + "WHERE s.environmentCrn = :environmentCrn "
            + "AND s.type in :stackTypes "
            + "AND s.terminated IS null"
    )
    List<StackViewDelegate> findAllByEnvironmentCrnAndStackType(@Param("environmentCrn") String environmentCrn,
            @Param("stackTypes") List<StackType> stackTypes);

    @Query("SELECT sc FROM SecurityConfig sc " +
            "LEFT JOIN sc.stack s " +
            "WHERE s.id = :stackId")
    SecurityConfig getSecurityByStackId(@Param("stackId") Long stackId);
}
