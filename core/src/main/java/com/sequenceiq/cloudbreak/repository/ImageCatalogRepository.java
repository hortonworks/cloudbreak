package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = ImageCatalog.class)
@Transactional(TxType.REQUIRED)
public interface ImageCatalogRepository extends WorkspaceResourceRepository<ImageCatalog, Long> {

    Set<ImageCatalog> findAllByWorkspaceIdAndArchived(Long workspaceId, boolean archived);

    Optional<ImageCatalog> findByResourceCrnAndArchivedFalse(String resourceCrn);

}
