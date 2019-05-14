package com.sequenceiq.environment.proxy;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.workspace.repository.check.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.environment.environment.repository.EnvironmentResourceRepository;

@EntityType(entityClass = ProxyConfig.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
@WorkspaceResourceType(resource = WorkspaceResource.PROXY)
interface ProxyConfigRepository extends EnvironmentResourceRepository<ProxyConfig, Long> {

    @Override
    @CheckPermissionsByWorkspaceId
    @Query("SELECT p FROM ProxyConfig p WHERE p.workspace.id = :workspaceId")
    Set<ProxyConfig> findAllByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
