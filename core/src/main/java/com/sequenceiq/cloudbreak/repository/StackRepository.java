package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.authorization.ResourceAction.READ;
import static com.sequenceiq.cloudbreak.repository.snippets.ShowTerminatedClustersSnippets.SHOW_TERMINATED_CLUSTERS_IF_REQUESTED;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspace;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.aspect.workspace.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = Stack.class)
@Transactional(TxType.REQUIRED)
@WorkspaceResourceType(resource = WorkspaceResource.STACK)
public interface StackRepository extends WorkspaceResourceRepository<Stack, Long> {

    @CheckPermissionsByReturnValue
    @Query("SELECT s from Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.cluster.ambariIp= :ambariIp AND s.terminated = null "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findByAmbari(@Param("ambariIp") String ambariIp);

    @CheckPermissionsByWorkspaceId(action = READ)
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.workspace.id= :workspaceId AND s.terminated = null "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<Stack> findForWorkspaceIdWithLists(@Param("workspaceId") Long workspaceId);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.name= :name AND s.workspace.id= :workspaceId AND s.terminated = null "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findByNameAndWorkspaceId(@Param("name") String name, @Param("workspaceId") Long workspaceId);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.name in :names AND s.workspace.id= :workspaceId AND s.terminated = null "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<Stack> findByNameInAndWorkspaceId(@Param("names") Set<String> name, @Param("workspaceId") Long workspaceId);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.name= :name AND s.workspace.id= :workspaceId AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED)
    Optional<Stack> findByNameAndWorkspaceIdWithLists(@Param("name") String name, @Param("workspaceId") Long workspaceId,
            @Param("showTerminated") Boolean showTerminated,
            @Param("terminatedAfter") Long terminatedAfter);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE s.id= :id "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findOneWithLists(@Param("id") Long id);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "LEFT JOIN FETCH s.cluster c LEFT JOIN FETCH c.components WHERE s.id= :id AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findOneWithCluster(@Param("id") Long id);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s "
            + "WHERE s.datalakeResourceId= :id AND s.terminated = null AND s.stackStatus.status <> 'DELETE_IN_PROGRESS' "
            + "AND s.stackStatus.status <> 'REQUESTED' "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<Stack> findEphemeralClusters(@Param("id") Long id);

    @CheckPermissionsByReturnValue
    @Query("SELECT distinct s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig WHERE ig.template.id= :templateId "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    List<Stack> findAllStackForTemplate(@Param("templateId") Long templateId);

    @DisableCheckPermissions
    @Query("SELECT s.id,s.stackStatus.status FROM Stack s WHERE s.id IN (:ids) AND (s.type is not 'TEMPLATE' OR s.type is null)")
    List<Object[]> findStackStatusesWithoutAuth(@Param("ids") Set<Long> ids);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.cluster.id= :clusterId AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findStackForCluster(@Param("clusterId") Long clusterId);

    @CheckPermissionsByWorkspace(action = READ, workspaceIndex = 1)
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.workspace= :workspace and s.name= :name AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Optional<Stack> findByNameInWorkspaceWithLists(@Param("name") String name, @Param("workspace") Workspace workspace);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.terminated = null AND (s.type is not 'TEMPLATE' OR s.type is null)")
    List<Stack> findAllAlive();

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.terminated = null AND "
            + "(s.workspace = null OR s.creator = null) AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<Stack> findAllAliveWithNoWorkspaceOrUser();

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.terminated = null AND s.stackStatus.status <> 'REQUESTED' "
            + "AND s.stackStatus.status <> 'CREATE_IN_PROGRESS' AND (s.type is not 'TEMPLATE' OR s.type is null)")
    List<Stack> findAllAliveAndProvisioned();

    @CheckPermissionsByWorkspaceId(action = READ)
    @Query("SELECT s FROM Stack s WHERE s.terminated = null AND s.workspace.id= :workspaceId AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<Stack> findAllForWorkspace(@Param("workspaceId") Long workspaceId);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status IN :statuses AND (s.type is not 'TEMPLATE' OR s.type is null)")
    List<Stack> findByStatuses(@Param("statuses") List<Status> statuses);

    @CheckPermissionsByReturnValue
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
            + "u.userId as userId "
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
            + "AND c.ambariIp IS NOT NULL "
            + "AND c.status = 'AVAILABLE' "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<AutoscaleStack> findAliveOnesWithAmbari();

    @DisableCheckPermissions
    Long countByCredential(Credential credential);

    @DisableCheckPermissions
    Set<Stack> findByCredential(Credential credential);

    @DisableCheckPermissions
    Long countByNetwork(Network network);

    @CheckPermissionsByReturnValue
    Set<Stack> findByNetwork(Network network);

    @DisableCheckPermissions
    @Query("SELECT COUNT(s) FROM Stack s WHERE (s.workspace = null OR s.creator = null) "
            + "AND s.terminated = null "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Long countStacksWithNoWorkspaceOrCreator();

    @CheckPermissionsByWorkspaceId
    @Query("SELECT COUNT(s) FROM Stack s WHERE s.workspace.id = :workspaceId AND s.environment.id = :environmentId "
            + "AND s.terminated = null "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Long countAliveOnesByWorkspaceAndEnvironment(@Param("workspaceId") Long workspaceId, @Param("environmentId") Long environmentId);

    @DisableCheckPermissions
    @Query("SELECT s.workspace.id FROM Stack s where s.id = :id")
    Long findWorkspaceIdById(@Param("id") Long id);

    @CheckPermissionsByWorkspaceId
    @Query("SELECT s.name FROM Stack s WHERE s.workspace.id = :workspaceId AND s.environment.id = :envId "
            + "AND s.type = 'DATALAKE' AND s.terminated = null")
    Set<String> findDatalakeStackNamesByWorkspaceAndEnvironment(@Param("workspaceId") Long workspaceId, @Param("envId") Long envId);

    @CheckPermissionsByWorkspaceId
    @Query("SELECT s.name FROM Stack s WHERE s.workspace.id = :workspaceId AND s.environment.id = :envId "
            + "AND s.type = 'WORKLOAD' AND s.terminated = null")
    Set<String> findWorkloadStackNamesByWorkspaceAndEnvironment(@Param("workspaceId") Long workspaceId, @Param("envId") Long envId);

    @DisableCheckPermissions
    @Query("SELECT s.workspace FROM Stack s where s.id = :id")
    Optional<Workspace> findWorkspaceById(@Param("id") Long id);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE s.id= :id "
            + "AND s.type is 'TEMPLATE'")
    Optional<Stack> findTemplateWithLists(@Param("id") Long id);

}
