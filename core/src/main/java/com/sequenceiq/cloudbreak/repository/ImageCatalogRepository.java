package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.authorization.service.model.projection.ResourceCrnAndNameView;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = ImageCatalog.class)
@Transactional(TxType.REQUIRED)
public interface ImageCatalogRepository extends WorkspaceResourceRepository<ImageCatalog, Long> {

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(i.id, i.resourceCrn) FROM ImageCatalog i " +
            "WHERE i.workspace.id = :workspaceId AND i.archived = false AND i.imageCatalogUrl is not null")
    List<ResourceWithId> findAsAuthorizationResourcesInWorkspace(@Param("workspaceId") Long workspaceId);

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(i.id, i.resourceCrn) FROM ImageCatalog i " +
            "WHERE i.workspace.id = :workspaceId AND i.archived = false AND i.imageCatalogUrl is null")
    List<ResourceWithId> findCustomAsAuthorizationResourcesInWorkspace(@Param("workspaceId") Long workspaceId);

    Set<ImageCatalog> findAllByWorkspaceIdAndArchivedAndImageCatalogUrlIsNull(Long workspaceId, boolean archived);

    Set<ImageCatalog> findAllByWorkspaceIdAndArchivedAndImageCatalogUrlIsNotNull(Long workspaceId, boolean archived);

    @Query("SELECT i.resourceCrn FROM ImageCatalog i " +
            "WHERE i.workspace.tenant.name = :tenantId AND i.name IN (:names) AND i.imageCatalogUrl is not null")
    List<String> findAllResourceCrnsByNamesAndTenantId(@Param("names") Collection<String> names, @Param("tenantId") String tenantId);

    @Query("SELECT i.resourceCrn FROM ImageCatalog i " +
            "WHERE i.workspace.tenant.name = :tenantId AND i.name = :name")
    Optional<String> findResourceCrnByNameAndTenantId(@Param("name") String name, @Param("tenantId") String tenantId);

    @Query("SELECT i.name as name, i.resourceCrn as crn FROM ImageCatalog i " +
            "WHERE i.workspace.tenant.name = :tenantId AND i.resourceCrn IN (:resourceCrns) AND i.imageCatalogUrl is not null")
    List<ResourceCrnAndNameView> findResourceNamesByCrnAndTenantId(@Param("resourceCrns") Collection<String> resourceCrns, @Param("tenantId") String tenantId);

    @Query("SELECT i.resourceCrn FROM ImageCatalog i " +
            "WHERE i.workspace.tenant.name = :tenantId AND i.imageCatalogUrl is not null")
    List<String> findAllResourceCrnsByTenantId(@Param("tenantId") String tenantId);

    Optional<ImageCatalog> findByResourceCrnAndArchivedFalseAndImageCatalogUrlIsNotNull(String resourceCrn);

    @Query("SELECT i FROM ImageCatalog i "
            + "LEFT JOIN FETCH i.workspace w "
            + "LEFT JOIN FETCH w.tenant t "
            + "WHERE i.id IN :ids "
            + "AND i.archived = false")
    Set<ImageCatalog> findAllByIdNotArchived(@Param("ids") List<Long> ids);

}
