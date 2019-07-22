package com.sequenceiq.cloudbreak.repository.cluster;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.repository.DisableCheckPermissions;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByWorkspaceId;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@DisableHasPermission
@EntityType(entityClass = DatalakeResources.class)
@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.DATAHUB)
public interface DatalakeResourcesRepository extends WorkspaceResourceRepository<DatalakeResources, Long> {

    @CheckPermissionsByReturnValue
    Optional<DatalakeResources> findByDatalakeStackId(Long datalakeStackId);

    @CheckPermissionsByWorkspaceId
    @Query("SELECT dr FROM DatalakeResources dr WHERE dr.workspace.id = :workspaceId AND dr.environmentCrn = :envCrn")
    Set<DatalakeResources> findDatalakeResourcesByWorkspaceAndEnvironment(@Param("workspaceId") Long workspaceId, @Param("envCrn") String envCrn);

    @CheckPermissionsByWorkspaceId
    @Query("SELECT dr.name FROM DatalakeResources dr WHERE dr.workspace.id = :workspaceId AND dr.environmentCrn = :envCrn")
    Set<String> findDatalakeResourcesNamesByWorkspaceAndEnvironment(@Param("workspaceId") Long workspaceId, @Param("envCrn") String envCrn);

    @DisableCheckPermissions
    Long countDatalakeResourcesByEnvironmentCrn(String environmentCrn);

}
