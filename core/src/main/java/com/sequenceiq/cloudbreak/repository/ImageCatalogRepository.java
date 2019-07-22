package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByReturnValue;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = ImageCatalog.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
@AuthorizationResourceType(resource = AuthorizationResource.DATAHUB)
public interface ImageCatalogRepository extends WorkspaceResourceRepository<ImageCatalog, Long> {

    @CheckPermissionsByReturnValue
    Set<ImageCatalog> findAllByWorkspaceIdAndArchived(Long workspaceId, boolean archived);

    @CheckPermissionsByReturnValue
    Optional<ImageCatalog> findByResourceCrnAndArchivedFalse(String resourceCrn);

}
