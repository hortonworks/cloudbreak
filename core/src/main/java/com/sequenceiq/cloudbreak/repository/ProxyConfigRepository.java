package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = ProxyConfig.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
@WorkspaceResourceType(resource = WorkspaceResource.PROXY)
public interface ProxyConfigRepository extends EnvironmentResourceRepository<ProxyConfig, Long> {

    @Override
    @CheckPermissionsByWorkspaceId
    @Query("SELECT p FROM ProxyConfig p WHERE p.workspace.id = :workspaceId")
    Set<ProxyConfig> findAllByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
