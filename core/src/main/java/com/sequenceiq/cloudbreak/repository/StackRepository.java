package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action.READ;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganization;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganizationId;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.organization.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = Stack.class)
@Transactional(TxType.REQUIRED)
@OrganizationResourceType(resource = OrganizationResource.STACK)
public interface StackRepository extends OrganizationResourceRepository<Stack, Long> {

    @DisableCheckPermissions
    @Query("SELECT s from Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.cluster.ambariIp= :ambariIp AND s.stackStatus.status <> 'DELETE_COMPLETED'")
    Stack findByAmbari(@Param("ambariIp") String ambariIp);

    @CheckPermissionsByOrganizationId(action = READ)
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.organization.id= :orgId AND s.stackStatus.status <> 'DELETE_COMPLETED'")
    Set<Stack> findForOrganizationIdWithLists(@Param("orgId") Long orgId);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s WHERE s.name= :name AND s.organization.id= :orgId AND s.stackStatus.status <> 'DELETE_COMPLETED'")
    Stack findByNameAndOrganizationId(@Param("name") String name, @Param("orgId") Long orgId);

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.name= :name AND s.organization.id= :orgId AND s.stackStatus.status <> 'DELETE_COMPLETED'")
    Stack findByNameAndOrganizationIdWithLists(@Param("name") String name, @Param("orgId") Long orgId);

    @DisableCheckPermissions
    @Query("SELECT c FROM Stack c LEFT JOIN FETCH c.resources LEFT JOIN FETCH c.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE c.id= :id")
    Stack findOneWithListsWithoutAuthorization(@Param("id") Long id);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Stack c LEFT JOIN FETCH c.resources LEFT JOIN FETCH c.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE c.id= :id")
    Stack findOneWithLists(@Param("id") Long id);

    @DisableCheckPermissions
    @Query("SELECT s FROM Stack s WHERE s.id= :id")
    Optional<Stack> findByIdWithoutAuth(@Param("id") Long id);

    @DisableCheckPermissions
    @Query("SELECT s FROM Stack s "
            + "WHERE s.datalakeId= :id AND s.stackStatus.status <> 'DELETE_COMPLETED' AND s.stackStatus.status <> 'DELETE_IN_PROGRESS'"
            + "AND s.stackStatus.status <> 'REQUESTED'")
    Set<Stack> findEphemeralClustersWithoutAuth(@Param("id") Long id);

    @DisableCheckPermissions
    @Query("SELECT distinct c FROM Stack c LEFT JOIN FETCH c.instanceGroups ig WHERE ig.template.id= :templateId")
    List<Stack> findAllStackForTemplateWithoutAuth(@Param("templateId") Long templateId);

    @DisableCheckPermissions
    @Query("SELECT s.id,s.stackStatus.status FROM Stack s WHERE s.id IN (:ids)")
    List<Object[]> findStackStatusesWithoutAuth(@Param("ids") Set<Long> ids);

    @DisableCheckPermissions
    @Query("SELECT c FROM Stack c WHERE c.cluster.id= :clusterId")
    Stack findStackForClusterWithoutAuth(@Param("clusterId") Long clusterId);

    @CheckPermissionsByOrganization(action = READ, organizationIndex = 1)
    @Query("SELECT t FROM Stack t LEFT JOIN FETCH t.resources LEFT JOIN FETCH t.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE t.organization= :organization and t.name= :name")
    Stack findByNameInOrganizationWithLists(@Param("name") String name, @Param("organization") Organization organization);

    @DisableCheckPermissions
    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status <> 'DELETE_COMPLETED'")
    List<Stack> findAllAliveWithoutAuth();

    @DisableCheckPermissions
    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status <> 'DELETE_COMPLETED' AND s.stackStatus.status <> 'REQUESTED' "
            + "AND s.stackStatus.status <> 'CREATE_IN_PROGRESS'")
    List<Stack> findAllAliveAndProvisionedWithoutAuth();

    @CheckPermissionsByOrganizationId(action = READ)
    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status <> 'DELETE_COMPLETED' AND s.organization.id= :organizationId")
    Set<Stack> findAllForOrganization(@Param("organizationId") Long organizationId);

    @DisableCheckPermissions
    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status IN :statuses")
    List<Stack> findByStatusesWithoutAuth(@Param("statuses") List<Status> statuses);

    @DisableCheckPermissions
    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.cluster LEFT JOIN FETCH s.credential LEFT JOIN FETCH s.network LEFT JOIN FETCH s.orchestrator "
            + "LEFT JOIN FETCH s.stackStatus LEFT JOIN FETCH s.securityConfig LEFT JOIN FETCH s.failurePolicy LEFT JOIN FETCH"
            + " s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE s.stackStatus.status <> 'DELETE_COMPLETED' "
            + "AND s.stackStatus.status <> 'DELETE_IN_PROGRESS'")
    Set<Stack> findAliveOnesWithoutAuthorization();

    @DisableCheckPermissions
    Long countByFlexSubscription(FlexSubscription flexSubscription);

    @DisableCheckPermissions
    Long countByCredential(Credential credential);

    @DisableCheckPermissions
    Set<Stack> findByCredential(Credential credential);

    @DisableCheckPermissions
    Long countByNetwork(Network network);

    @DisableCheckPermissions
    Set<Stack> findByNetwork(Network network);
}
