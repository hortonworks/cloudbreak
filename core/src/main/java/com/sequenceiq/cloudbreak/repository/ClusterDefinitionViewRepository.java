package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.view.ClusterDefinitionView;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = ClusterDefinitionView.class)
@Transactional(TxType.REQUIRED)
@WorkspaceResourceType(resource = WorkspaceResource.CLUSTER_DEFINITION)
public interface ClusterDefinitionViewRepository extends WorkspaceResourceRepository<ClusterDefinitionView, Long> {

    @Query("SELECT b FROM ClusterDefinitionView b WHERE b.workspace.id= :workspaceId AND b.status <> 'DEFAULT_DELETED'")
    @CheckPermissionsByReturnValue
    Set<ClusterDefinitionView> findAllByNotDeletedInWorkspace(@Param("workspaceId") Long workspaceId);
}
