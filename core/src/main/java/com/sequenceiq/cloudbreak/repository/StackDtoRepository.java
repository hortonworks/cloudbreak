package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.repository.snippets.ShowTerminatedClustersSnippets.SHOW_TERMINATED_CLUSTERS_IF_REQUESTED;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.StackDto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@org.springframework.stereotype.Repository
public interface StackDtoRepository extends Repository<Stack, Long> {

    String PROJECTION = "s.id as id, "
            + "s.resourceCrn as resourceCrn, "
            + "s.name as name, "
            + "s.region as region, "
            + "s.gatewayPort as gatewayPort, "
            + "s.tunnel as tunnel, "
            + "s.environmentCrn as environmentCrn, "
            + "s.type as type, "
            //+ "CASE WHEN (s.stackVersion is not null) THEN s.stackVersion ELSE b.stackVersion END as stackVersion,"
            + "ss.status as stackStatus, "
            + "ss.statusReason as statusReason, "
            + "s.cloudPlatform as cloudPlatform, "
            + "s.created as created, "
            + "s.datalakeCrn as datalakeCrn, "
            + "s.tags as tags,"
            + "s.cluster.id as clusterId, "
            + "s.platformVariant as platformVariant, "
            + "s.customDomain as customDomain, "
            + "s.customHostname as customHostname, "
            + "s.hostgroupNameAsHostname as hostgroupNameAsHostname, "
            + "s.clusterNameAsSubdomain as clusterNameAsSubdomain, "
            + "s.displayName as displayName, "
            + "s.description as description, "
            + "s.externalDatabaseCreationType as externalDatabaseCreationType, "
            + "s.externalDatabaseEngineVersion as externalDatabaseEngineVersion, "
            + "a.loginUserName as loginUserName, "
            + "a.publicKey as publicKey, "
            + "a.publicKeyId as publicKeyId, "

            + "w.id as workspaceId, "

            /*+ "b.id as blueprintId, "
            + "b.hostGroupCount as hostGroupCount, "
            + "b.status as blueprintStatus, "
            + "b.blueprintUpgradeOption as blueprintUpgradeOption, "
            + "s.terminated as terminated, "*/
            + "u.id as creatorId, "
            + "u.userId as creatorUserId, "
            + "u.userName as creatorUsername, "
            + "u.userCrn as creatorUserCrn, "
            + "sc as securityConfig ";

    @Query("SELECT "
            + PROJECTION
//            + "c.certExpirationState as certExpirationState "
            + "FROM Stack s "
//            + "LEFT JOIN s.cluster c "
            //+ "LEFT JOIN c.blueprint b "
            + "LEFT JOIN s.stackStatus ss "
            + "LEFT JOIN s.creator u "
            + "LEFT JOIN s.workspace w "
            + "LEFT JOIN s.stackAuthentication a "
            + "LEFT JOIN s.securityConfig sc "
            + "WHERE w.id= :workspaceId "
            + "AND s.name = :name "
            + "AND (s.type IS null OR s.type = :stackType) "
            + "AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED
//            + "AND (:environmentCrn IS null OR s.environmentCrn = :environmentCrn) "
    )
    Optional<StackDto> findByName(@Param("workspaceId") long workspaceId, @Param("name") String name,  @Param("stackType") StackType stackType,
            @Param("showTerminated") Boolean showTerminated, @Param("terminatedAfter") Long terminatedAfter);

    @Query("SELECT "
            + PROJECTION
//            + "c.certExpirationState as certExpirationState "
            + "FROM Stack s "
//            + "LEFT JOIN s.cluster c "
            //+ "LEFT JOIN c.blueprint b "
            + "LEFT JOIN s.stackStatus ss "
            + "LEFT JOIN s.creator u "
            + "LEFT JOIN s.workspace w "
            + "LEFT JOIN s.stackAuthentication a "
            + "LEFT JOIN s.securityConfig sc "
            + "WHERE w.id= :workspaceId "
            + "AND s.name = :name "
            + "AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED
//            + "AND (:environmentCrn IS null OR s.environmentCrn = :environmentCrn) "
    )
    Optional<StackDto> findByName(@Param("workspaceId") long workspaceId, @Param("name") String name,
            @Param("showTerminated") Boolean showTerminated, @Param("terminatedAfter") Long terminatedAfter);

    @Query("SELECT "
            + PROJECTION
//            + "c.certExpirationState as certExpirationState "
            + "FROM Stack s "
//            + "LEFT JOIN s.cluster c "
            //+ "LEFT JOIN c.blueprint b "
            + "LEFT JOIN s.stackStatus ss "
            + "LEFT JOIN s.creator u "
            + "LEFT JOIN s.workspace w "
            + "LEFT JOIN s.stackAuthentication a "
            + "LEFT JOIN s.securityConfig sc "
            + "WHERE w.id= :workspaceId "
            + "AND s.name = :name "
            + "AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED
//            + "AND (:environmentCrn IS null OR s.environmentCrn = :environmentCrn) "
    )
    Optional<StackDto> findByCrn(@Param("workspaceId") long workspaceId, @Param("name") String name,
            @Param("showTerminated") Boolean showTerminated, @Param("terminatedAfter") Long terminatedAfter);

    @Query("SELECT "
            + PROJECTION
//            + "c.certExpirationState as certExpirationState "
            + "FROM Stack s "
//            + "LEFT JOIN s.cluster c "
            //+ "LEFT JOIN c.blueprint b "
            + "LEFT JOIN s.stackStatus ss "
            + "LEFT JOIN s.creator u "
            + "LEFT JOIN s.workspace w "
            + "LEFT JOIN s.stackAuthentication a "
            + "LEFT JOIN s.securityConfig sc "
            + "WHERE w.id= :workspaceId "
            + "AND s.name = :name "
            + "AND (s.type IS null OR s.type = :stackType) "
            + "AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED
//            + "AND (:environmentCrn IS null OR s.environmentCrn = :environmentCrn) "
    )
    Optional<StackDto> findByCrn(@Param("workspaceId") long workspaceId, @Param("name") String name,  @Param("stackType") StackType stackType,
            @Param("showTerminated") Boolean showTerminated, @Param("terminatedAfter") Long terminatedAfter);

    @Query("SELECT n FROM Stack s " +
            "LEFT JOIN s.network n " +
            "WHERE s.id = :stackId")
    Optional<Network> getNetworkByStackById(@Param("stackId") long stackId);

    @Query("SELECT "
            + PROJECTION
            + "FROM Stack s "
            + "LEFT JOIN s.stackStatus ss "
            + "LEFT JOIN s.creator u "
            + "LEFT JOIN s.workspace w "
            + "LEFT JOIN s.stackAuthentication a "
            + "LEFT JOIN s.securityConfig sc "
            + "WHERE s.id = :stackId "
    )
    Optional<StackDto> findById(@Param("stackId") long stackId);
}
