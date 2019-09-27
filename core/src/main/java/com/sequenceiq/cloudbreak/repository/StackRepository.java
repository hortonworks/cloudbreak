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
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.aspect.workspace.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = Stack.class)
@Transactional(TxType.REQUIRED)
@WorkspaceResourceType(resource = WorkspaceResource.STACK)
public interface StackRepository extends WorkspaceResourceRepository<Stack, Long> {

    @CheckPermissionsByReturnValue
    @Query("SELECT s from Stack s LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.cluster.ambariIp= :ambariIp AND s.terminated = null")
    Stack findByAmbari(@Param("ambariIp") String ambariIp);

    @CheckPermissionsByWorkspaceId(action = READ)
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.workspace.id= :workspaceId AND s.terminated = null")
    Set<Stack> findForWorkspaceIdWithLists(@Param("workspaceId") Long workspaceId);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.name= :name AND s.workspace.id= :workspaceId AND s.terminated = null")
    Stack findByNameAndWorkspaceId(@Param("name") String name, @Param("workspaceId") Long workspaceId);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData imd "
            + "WHERE s.name= :name AND s.workspace.id= :workspaceId AND s.terminated = null "
            + "AND (imd.instanceStatus <> 'TERMINATED' AND imd.terminationDate = null)")
    Stack findByNameAndWorkspaceIdWithLists(@Param("name") String name, @Param("workspaceId") Long workspaceId);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Stack c LEFT JOIN FETCH c.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE c.id= :id")
    Stack findOneWithLists(@Param("id") Long id);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s "
            + "WHERE s.datalakeId= :id AND s.terminated = null AND s.stackStatus.status <> 'DELETE_IN_PROGRESS'"
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

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.terminated = null")
    List<Stack> findAllAlive();

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.terminated = null AND "
            + "(s.workspace = null OR s.creator = null)")
    Set<Stack> findAllAliveWithNoWorkspaceOrUser();

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.terminated = null AND s.stackStatus.status <> 'REQUESTED' "
            + "AND s.stackStatus.status <> 'CREATE_IN_PROGRESS'")
    List<Stack> findAllAliveAndProvisioned();

    @CheckPermissionsByWorkspaceId(action = READ)
    @Query("SELECT s FROM Stack s WHERE s.terminated = null AND s.workspace.id= :workspaceId")
    Set<Stack> findAllForWorkspace(@Param("workspaceId") Long workspaceId);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status IN :statuses")
    List<Stack> findByStatuses(@Param("statuses") List<Status> statuses);

    @CheckPermissionsByReturnValue
    @Query("SELECT s.id as id, s.name as name, s.owner as owner, s.gatewayPort as gatewayPort, s.created as created, ss.status as stackStatus, "
            + "c.cloudbreakAmbariUser as cloudbreakAmbariUser, c.cloudbreakAmbariPassword as cloudbreakAmbariPassword, c.status as clusterStatus, "
            + "ig.instanceGroupType as instanceGroupType, im.instanceMetadataType as instanceMetadataType, im.publicIp as publicIp, "
            + "im.privateIp as privateIp, sc.usePrivateIpToTls as usePrivateIpToTls "
            + "FROM Stack s LEFT JOIN s.cluster c "
            + "LEFT JOIN s.stackStatus ss "
            + "LEFT JOIN s.instanceGroups ig "
            + "LEFT JOIN ig.instanceMetaData im "
            + "LEFT JOIN s.securityConfig sc "
            + "WHERE instanceGroupType = 'GATEWAY' AND instanceMetadataType = 'GATEWAY_PRIMARY' AND s.terminated = null AND im.instanceStatus <> 'TERMINATED'")
    Set<AutoscaleStack> findAliveOnes();

    @DisableCheckPermissions
    Long countByFlexSubscription(FlexSubscription flexSubscription);

    @DisableCheckPermissions
    Long countByCredential(Credential credential);

    @DisableCheckPermissions
    Set<Stack> findByCredential(Credential credential);

    @CheckPermissionsByReturnValue
    Set<Stack> findByNetwork(Network network);

    @DisableCheckPermissions
    @Query("SELECT COUNT(s) FROM Stack s WHERE (s.workspace = null OR s.creator = null) "
            + "AND s.terminated = null")
    Long countStacksWithNoWorkspaceOrCreator();

    @DisableCheckPermissions
    @Query("SELECT COUNT(s) FROM Stack s WHERE s.account = :account AND s.terminated = null")
    Long countActiveByAccount(@Param("account") String account);

    @DisableCheckPermissions
    @Query("SELECT COUNT(s) FROM Stack s WHERE s.owner = :owner AND s.terminated = null")
    Long countActiveByOwner(@Param("owner") String owner);

    @DisableCheckPermissions
    @Query("SELECT s.workspace.id FROM Stack s where s.id = :id")
    Long findWorkspaceIdById(@Param("id") Long id);

    @CheckPermissionsByWorkspaceId
    @Query("SELECT s.id as id, s.name as name, b.name as blueprintName, b.stackType as stackType, b.stackVersion as stackVersion, ss.status as stackStatus, "
            + "s.platformVariant as cloudPlatform, c.status as clusterStatus, s.created as created, s.datalakeId as sharedClusterId, b.tags as blueprintTags,"
            + "crd.govCloud as govCloud "
            + "FROM Stack s LEFT JOIN s.cluster c LEFT JOIN c.blueprint b LEFT JOIN s.stackStatus ss LEFT JOIN s.credential crd "
            + "WHERE s.workspace.id= :id AND s.terminated = null")
    Set<StackListItem> findByWorkspaceId(@Param("id") Long id);

    @DisableCheckPermissions
    @Query("SELECT s.id FROM Stack s WHERE s.name= :name AND s.workspace.id= :workspaceId AND s.terminated = null")
    Long findIdByNameAndWorkspaceId(@Param("name") String name, @Param("workspaceId") Long workspaceId);

    @DisableCheckPermissions
    @Query("SELECT s.name FROM Stack s WHERE s.id= :id AND s.workspace.id= :workspaceId")
    String findNameByIdAndWorkspaceId(@Param("id") Long sharedClusterId, @Param("workspaceId") Long workspaceId);
}
