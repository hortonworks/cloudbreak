package com.sequenceiq.cloudbreak.repository.cluster;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.repository.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByWorkspaceId;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.authorization.resource.AuthorizationResource;

@DisableHasPermission
@Transactional(TxType.REQUIRED)
@EntityType(entityClass = Cluster.class)
@AuthorizationResourceType(resource = AuthorizationResource.DATAHUB)
public interface ClusterRepository extends WorkspaceResourceRepository<Cluster, Long> {

    @Override
    @DisableCheckPermissions
    Cluster save(Cluster entity);

    @CheckPermissionsByReturnValue
    Optional<Cluster> findOneByStackId(long stackId);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.gateway LEFT JOIN FETCH c.hostGroups LEFT JOIN FETCH c.containers LEFT JOIN FETCH c.components "
            + "LEFT JOIN FETCH c.rdsConfigs WHERE c.id= :id")
    Optional<Cluster> findOneWithLists(@Param("id") Long id);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Cluster c WHERE c.stack IS NOT NULL AND c.stack.terminated IS NULL AND c.status IN :statuses")
    List<Cluster> findByStatuses(@Param("statuses") Collection<Status> statuses);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.stack WHERE c.workspace = null")
    Set<Cluster> findAllWithNoWorkspace();

    @CheckPermissionsByReturnValue
    Set<Cluster> findByBlueprint(Blueprint blueprint);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Cluster c INNER JOIN c.rdsConfigs rc WHERE rc.id= :id AND c.status != 'DELETE_COMPLETED'")
    Set<Cluster> findByRdsConfig(@Param("id") Long rdsConfigId);

    @CheckPermissionsByReturnValue
    @Query("SELECT c.name FROM Cluster c INNER JOIN c.rdsConfigs rc WHERE rc.id= :id AND c.status != 'DELETE_COMPLETED'")
    Set<String> findNamesByRdsConfig(@Param("id") Long rdsConfigId);

    @CheckPermissionsByWorkspaceId
    @Query("SELECT COUNT(c) FROM Cluster c WHERE c.workspace.id = :workspaceId AND c.environmentCrn = :environmentCrn AND c.status != 'DELETE_COMPLETED'")
    Long countAliveOnesByWorkspaceAndEnvironment(@Param("workspaceId") Long workspaceId, @Param("environmentCrn") String environmentCrn);
}
