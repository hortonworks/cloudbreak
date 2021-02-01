package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.service.list.AuthorizationResource;
import com.sequenceiq.authorization.service.model.projection.ResourceCrnAndNameView;
import com.sequenceiq.cloudbreak.domain.view.RecipeView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = RecipeView.class)
@Transactional(TxType.REQUIRED)
public interface RecipeViewRepository extends WorkspaceResourceRepository<RecipeView, Long> {

    @Query("SELECT i.resourceCrn FROM RecipeView i WHERE i.workspace.tenant.name = :tenantId AND i.name IN (:names)")
    List<String> findAllResourceCrnsByNamesAndTenantId(@Param("names") Collection<String> names, @Param("tenantId") String tenantId);

    @Query("SELECT i.resourceCrn FROM RecipeView i WHERE i.workspace.tenant.name = :tenantId AND i.name = :name")
    Optional<String> findResourceCrnByNameAndTenantId(@Param("name") String name, @Param("tenantId") String tenantId);

    @Query("SELECT i.name as name, i.resourceCrn as crn FROM RecipeView i WHERE i.workspace.tenant.name = :tenantId AND i.resourceCrn IN (:resourceCrns)")
    List<ResourceCrnAndNameView> findResourceNamesByCrnAndTenantId(@Param("resourceCrns") Collection<String> resourceCrns, @Param("tenantId") String tenantId);

    @Query("SELECT i.resourceCrn FROM RecipeView i WHERE i.workspace.tenant.name = :tenantId")
    List<String> findAllResourceCrnsByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT new com.sequenceiq.authorization.service.list.AuthorizationResource(r.id, r.resourceCrn) FROM Recipe r WHERE r.workspace.id = :workspaceId " +
            "AND r.archived = false")
    List<AuthorizationResource> findAsAuthorizationResourcesInWorkspace(@Param("workspaceId") Long workspaceId);
}
