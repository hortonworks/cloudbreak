package com.sequenceiq.cloudbreak.repository.cluster;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.aspect.workspace.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = DatalakeResources.class)
@Transactional(Transactional.TxType.REQUIRED)
@WorkspaceResourceType(resource = WorkspaceResource.DATALAKE_RESOURCES)
public interface DatalakeResourcesRepository extends WorkspaceResourceRepository<DatalakeResources, Long> {
    @CheckPermissionsByReturnValue
    DatalakeResources findByDatalakeStackId(Long datalakeStackId);

    @CheckPermissionsByWorkspaceId
    @Query("SELECT dr.name FROM DatalakeResources dr WHERE dr.workspace.id = :workspaceId AND dr.environment.id = :envId")
    Set<String> findDatalakeResourcesNamesByWorkspaceAndEnvironment(@Param("workspaceId") Long workspaceId, @Param("envId") Long envId);

    @DisableCheckPermissions
    Long countDatalakeResourcesByEnvironment(EnvironmentView environment);
}
