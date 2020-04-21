package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = ImageCatalog.class)
@Transactional(TxType.REQUIRED)
public interface ImageCatalogRepository extends WorkspaceResourceRepository<ImageCatalog, Long> {

    Set<ImageCatalog> findAllByWorkspaceIdAndArchived(Long workspaceId, boolean archived);

    @Query("SELECT i.resourceCrn FROM ImageCatalog i WHERE i.workspace.tenant.name = :tenantId AND i.name IN (:names)")
    List<String> findAllResourceCrnsByNamesAndTenantId(@Param("names") Collection<String> names, @Param("tenantId") String tenantId);

    @Query("SELECT i.resourceCrn FROM ImageCatalog i WHERE i.workspace.tenant.name = :tenantId AND i.name = :name")
    Optional<String> findResourceCrnByNameAndTenantId(@Param("name") String name, @Param("tenantId") String tenantId);

    @Query("SELECT i.resourceCrn FROM ImageCatalog i WHERE i.workspace.tenant.name = :tenantId")
    List<String> findAllResourceCrnsByTenantId(@Param("tenantId") String tenantId);

    Optional<ImageCatalog> findByResourceCrnAndArchivedFalse(String resourceCrn);

}
