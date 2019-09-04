package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.repository.snippets.ShowTerminatedClustersSnippets.SHOW_TERMINATED_CLUSTERS_IF_REQUESTED;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = Stack.class)
@Transactional(TxType.REQUIRED)
public interface StackRepository extends WorkspaceResourceRepository<Stack, Long> {

    @Query("SELECT s from Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.cluster.clusterManagerIp= :clusterManagerIp AND s.terminated = null "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findByAmbari(@Param("clusterManagerIp") String clusterManagerIp);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.workspace.id= :workspaceId AND s.terminated = null "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<Stack> findForWorkspaceIdWithLists(@Param("workspaceId") Long workspaceId);

    @Query("SELECT s FROM Stack s WHERE s.name= :name AND s.workspace.id= :workspaceId AND s.terminated = null "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findByNameAndWorkspaceId(@Param("name") String name, @Param("workspaceId") Long workspaceId);

    @Query("SELECT s FROM Stack s WHERE s.resourceCrn= :crn AND s.workspace.id= :workspaceId AND s.terminated = null "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findByCrnAndWorkspaceId(@Param("crn") String crn, @Param("workspaceId") Long workspaceId);

    @Query("SELECT s FROM Stack s WHERE s.name in :names AND s.workspace.id= :workspaceId AND s.terminated = null "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<Stack> findByNameInAndWorkspaceId(@Param("names") Set<String> name, @Param("workspaceId") Long workspaceId);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.name= :name AND s.workspace.id= :workspaceId AND s.type = :type AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED)
    Optional<Stack> findByNameAndWorkspaceIdWithLists(@Param("name") String name, @Param("type") StackType type, @Param("workspaceId") Long workspaceId,
            @Param("showTerminated") Boolean showTerminated,
            @Param("terminatedAfter") Long terminatedAfter);

    @Query("SELECT s FROM Stack s WHERE s.environmentCrn= :environmentCrn AND s.type = :type AND s.terminated=null")
    List<Stack> findByEnvironmentCrnAndStackType(@Param("environmentCrn") String environmentCrn, @Param("type") StackType type);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.name= :name AND s.workspace.id= :workspaceId AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED)
    Optional<Stack> findByNameAndWorkspaceIdWithLists(@Param("name") String name, @Param("workspaceId") Long workspaceId,
            @Param("showTerminated") Boolean showTerminated,
            @Param("terminatedAfter") Long terminatedAfter);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.resourceCrn= :crn AND s.workspace.id= :workspaceId AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED)
    Optional<Stack> findByCrnAndWorkspaceIdWithLists(@Param("crn") String crn, @Param("workspaceId") Long workspaceId,
            @Param("showTerminated") Boolean showTerminated, @Param("terminatedAfter") Long terminatedAfter);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.resourceCrn= :crn AND s.workspace.id= :workspaceId AND s.type = :type AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED)
    Optional<Stack> findByCrnAndWorkspaceIdWithLists(@Param("crn") String crn, @Param("type") StackType type, @Param("workspaceId") Long workspaceId,
            @Param("showTerminated") Boolean showTerminated, @Param("terminatedAfter") Long terminatedAfter);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE s.id= :id "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findOneWithLists(@Param("id") Long id);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE s.resourceCrn= :crn "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findOneByCrnWithLists(@Param("crn") String crn);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "LEFT JOIN FETCH s.cluster c LEFT JOIN FETCH c.components WHERE s.id= :id AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findOneWithCluster(@Param("id") Long id);

    @Query("SELECT s FROM Stack s "
            + "WHERE s.datalakeResourceId= :id AND s.terminated = null AND s.stackStatus.status <> 'DELETE_IN_PROGRESS' "
            + "AND s.stackStatus.status <> 'REQUESTED' "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<Stack> findEphemeralClusters(@Param("id") Long id);

    @Query("SELECT distinct s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig WHERE ig.template.id= :templateId "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    List<Stack> findAllStackForTemplate(@Param("templateId") Long templateId);

    @Query("SELECT s.id,s.stackStatus.status FROM Stack s WHERE s.id IN (:ids) AND (s.type is not 'TEMPLATE' OR s.type is null)")
    List<Object[]> findStackStatusesWithoutAuth(@Param("ids") Set<Long> ids);

    @Query("SELECT s FROM Stack s WHERE s.cluster.id= :clusterId AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findStackForCluster(@Param("clusterId") Long clusterId);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.workspace= :workspace and s.name= :name AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findByNameInWorkspaceWithLists(@Param("name") String name, @Param("workspace") Workspace workspace);

    @Query("SELECT s FROM Stack s WHERE s.terminated = null AND (s.type is not 'TEMPLATE' OR s.type is null)")
    List<Stack> findAllAlive();

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig "
            + "WHERE s.terminated = null AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<Stack> findAllAliveWithInstanceGroups();

    @Query("SELECT s FROM Stack s WHERE s.terminated = null AND "
            + "(s.workspace = null OR s.creator = null) AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<Stack> findAllAliveWithNoWorkspaceOrUser();

    @Query("SELECT s FROM Stack s WHERE s.terminated = null AND s.stackStatus.status <> 'REQUESTED' "
            + "AND s.stackStatus.status <> 'CREATE_IN_PROGRESS' AND (s.type is not 'TEMPLATE' OR s.type is null)")
    List<Stack> findAllAliveAndProvisioned();

    @Query("SELECT s FROM Stack s WHERE s.terminated = null AND s.workspace.id= :workspaceId AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<Stack> findAllForWorkspace(@Param("workspaceId") Long workspaceId);

    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status IN :statuses AND (s.type is not 'TEMPLATE' OR s.type is null)")
    List<Stack> findByStatuses(@Param("statuses") List<Status> statuses);

    @Query("SELECT s.id as id, "
            + "s.name as name, "
            + "s.gatewayPort as gatewayPort, "
            + "s.created as created, "
            + "ss.status as stackStatus, "
            + "c.cloudbreakAmbariUser as cloudbreakAmbariUser, "
            + "c.cloudbreakAmbariPassword as cloudbreakAmbariPassword, "
            + "c.status as clusterStatus, "
            + "ig.instanceGroupType as instanceGroupType, "
            + "im.instanceMetadataType as instanceMetadataType, "
            + "im.publicIp as publicIp, "
            + "im.privateIp as privateIp, "
            + "sc.usePrivateIpToTls as usePrivateIpToTls, "
            + "w.id as workspaceId, "
            + "t.name as tenantName, "
            + "u.userId as userId, "
            + "s.resourceCrn as crn, "
            + "c.variant as clusterManagerVariant "
            + "FROM Stack s "
            + "LEFT JOIN s.cluster c "
            + "LEFT JOIN s.stackStatus ss "
            + "LEFT JOIN s.instanceGroups ig "
            + "LEFT JOIN ig.instanceMetaData im "
            + "LEFT JOIN s.securityConfig sc "
            + "LEFT JOIN s.workspace w "
            + "LEFT JOIN w.tenant t "
            + "LEFT JOIN s.creator u "
            + "WHERE instanceGroupType = 'GATEWAY' "
            + "AND instanceMetadataType = 'GATEWAY_PRIMARY' "
            + "AND s.terminated = null "
            + "AND c.clusterManagerIp IS NOT NULL "
            + "AND c.status = 'AVAILABLE' "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<AutoscaleStack> findAliveOnesWithAmbari();

    Set<Stack> findByNetwork(Network network);

    @Query("SELECT s.name FROM Stack s WHERE s.workspace.id = :workspaceId AND s.environmentCrn = :environmentCrn "
            + "AND s.terminated = null "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    List<String> findNamesOfAliveOnesByWorkspaceAndEnvironment(@Param("workspaceId") Long workspaceId, @Param("environmentCrn") String environmentCrn);

    @Query("SELECT COUNT(s) FROM Stack s WHERE s.workspace.id = :workspaceId AND s.environmentCrn = :environmentCrn "
            + "AND s.terminated = null "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Long countAliveOnesByWorkspaceAndEnvironment(@Param("workspaceId") Long workspaceId, @Param("environmentCrn") String environmentCrn);

    @Query("SELECT s FROM Stack s WHERE s.workspace.id = :workspaceId AND s.environmentCrn = :environmentCrn "
            + "AND s.terminated IS NOT null "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<Stack> findTerminatedByWorkspaceIdAndEnvironmentId(@Param("workspaceId") Long workspaceId, @Param("environmentCrn") Long environmentCrn);

    @Query("SELECT s.workspace.id FROM Stack s where s.id = :id")
    Long findWorkspaceIdById(@Param("id") Long id);

    @Query("SELECT s.workspace.id FROM Stack s where s.resourceCrn = :crn")
    Long findWorkspaceIdByCrn(@Param("crn") String crn);

    @Query("SELECT s.name FROM Stack s WHERE s.workspace.id = :workspaceId AND s.environmentCrn = :environmentCrn "
            + "AND s.type = 'DATALAKE' AND s.terminated = null")
    Set<String> findDatalakeStackNamesByWorkspaceAndEnvironment(@Param("workspaceId") Long workspaceId, @Param("environmentCrn") String environmentCrn);

    @Query("SELECT s.name FROM Stack s WHERE s.workspace.id = :workspaceId AND s.environmentCrn = :environmentCrn "
            + "AND s.type = 'WORKLOAD' AND s.terminated = null")
    Set<String> findWorkloadStackNamesByWorkspaceAndEnvironment(@Param("workspaceId") Long workspaceId, @Param("environmentCrn") String environmentCrn);

    @Query("SELECT s.workspace FROM Stack s where s.id = :id")
    Optional<Workspace> findWorkspaceById(@Param("id") Long id);

    @Query("SELECT s.workspace FROM Stack s where s.resourceCrn = :crn")
    Optional<Workspace> findWorkspaceByCrn(@Param("crn") String crn);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE s.id= :id "
            + "AND s.type is 'TEMPLATE'")
    Optional<Stack> findTemplateWithLists(@Param("id") Long id);

    @Query("SELECT s FROM Stack s WHERE s.resourceCrn = :crn")
    Optional<Stack> findByResourceCrn(@Param("crn") String crn);

}
