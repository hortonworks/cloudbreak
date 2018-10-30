package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.READ;

import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspace;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = Stack.class)
@Transactional(TxType.REQUIRED)
@WorkspaceResourceType(resource = WorkspaceResource.STACK)
public interface StackRepository extends WorkspaceResourceRepository<Stack, Long> {

    @CheckPermissionsByReturnValue
    @Query("SELECT s from Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.cluster.ambariIp= :ambariIp AND s.stackStatus.status <> 'DELETE_COMPLETED'")
    Stack findByAmbari(@Param("ambariIp") String ambariIp);

    @CheckPermissionsByWorkspaceId(action = READ)
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.workspace.id= :workspaceId AND s.stackStatus.status <> 'DELETE_COMPLETED'")
    Set<Stack> findForWorkspaceIdWithLists(@Param("workspaceId") Long workspaceId);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.name= :name AND s.workspace.id= :workspaceId AND s.stackStatus.status <> 'DELETE_COMPLETED'")
    Stack findByNameAndWorkspaceId(@Param("name") String name, @Param("workspaceId") Long workspaceId);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.name= :name AND s.workspace.id= :workspaceId AND s.stackStatus.status <> 'DELETE_COMPLETED'")
    Stack findByNameAndWorkspaceIdWithLists(@Param("name") String name, @Param("workspaceId") Long workspaceId);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Stack c LEFT JOIN FETCH c.resources LEFT JOIN FETCH c.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE c.id= :id")
    Stack findOneWithLists(@Param("id") Long id);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s "
            + "WHERE s.datalakeId= :id AND s.stackStatus.status <> 'DELETE_COMPLETED' AND s.stackStatus.status <> 'DELETE_IN_PROGRESS'"
            + "AND s.stackStatus.status <> 'REQUESTED'")
    Set<Stack> findEphemeralClusters(@Param("id") Long id);

    @CheckPermissionsByReturnValue
    @Query("SELECT distinct c FROM Stack c LEFT JOIN FETCH c.instanceGroups ig WHERE ig.template.id= :templateId")
    List<Stack> findAllStackForTemplate(@Param("templateId") Long templateId);

    @DisableCheckPermissions
    @Query("SELECT s.id,s.stackStatus.status FROM Stack s WHERE s.id IN (:ids)")
    List<Object[]> findStackStatusesWithoutAuth(@Param("ids") Set<Long> ids);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Stack c WHERE c.cluster.id= :clusterId")
    Stack findStackForCluster(@Param("clusterId") Long clusterId);

    @CheckPermissionsByWorkspace(action = READ, workspaceIndex = 1)
    @Query("SELECT t FROM Stack t LEFT JOIN FETCH t.resources LEFT JOIN FETCH t.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE t.workspace= :workspace and t.name= :name")
    Stack findByNameInWorkspaceWithLists(@Param("name") String name, @Param("workspace") Workspace workspace);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status <> 'DELETE_COMPLETED'")
    List<Stack> findAllAlive();

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status <> 'DELETE_COMPLETED' AND "
            + "(s.workspace = null OR s.creator = null)")
    Set<Stack> findAllAliveWithNoWorkspaceOrUser();

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status <> 'DELETE_COMPLETED' AND s.stackStatus.status <> 'REQUESTED' "
            + "AND s.stackStatus.status <> 'CREATE_IN_PROGRESS'")
    List<Stack> findAllAliveAndProvisioned();

    @CheckPermissionsByWorkspaceId(action = READ)
    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status <> 'DELETE_COMPLETED' AND s.workspace.id= :workspaceId")
    Set<Stack> findAllForWorkspace(@Param("workspaceId") Long workspaceId);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status IN :statuses")
    List<Stack> findByStatuses(@Param("statuses") List<Status> statuses);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.cluster LEFT JOIN FETCH s.credential LEFT JOIN FETCH s.network LEFT JOIN FETCH s.orchestrator "
            + "LEFT JOIN FETCH s.stackStatus LEFT JOIN FETCH s.securityConfig LEFT JOIN FETCH s.failurePolicy LEFT JOIN FETCH"
            + " s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE s.stackStatus.status <> 'DELETE_COMPLETED' "
            + "AND s.stackStatus.status <> 'DELETE_IN_PROGRESS' "
            + "AND s.cluster.ambariIp IS NOT NULL "
            + "AND s.cluster.status = 'AVAILABLE' ")
    Set<Stack> findAliveOnesWithAmbari();

    @DisableCheckPermissions
    Long countByFlexSubscription(FlexSubscription flexSubscription);

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
            + "AND s.stackStatus.status <> 'DELETE_COMPLETED'")
    Long countStacksWithNoWorkspaceOrCreator();

    @DisableCheckPermissions
    @Query("SELECT s.workspace.id FROM Stack s where s.id = :id")
    Long findWorkspaceIdById(@Param("id") Long id);

    @DisableCheckPermissions
    @Query("SELECT s.workspace FROM Stack s where s.id = :id")
    Workspace findWorkspaceById(@Param("id") Long id);
}
