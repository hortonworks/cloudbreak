package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.authorization.service.model.projection.ResourceCrnAndNameView;
import com.sequenceiq.cloudbreak.domain.view.RecipeView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = RecipeView.class)
@Transactional(TxType.REQUIRED)
public interface RecipeViewRepository extends WorkspaceResourceRepository<RecipeView, Long> {

    @Query("SELECT i.resourceCrn FROM RecipeView i WHERE i.workspace.tenant.name = :tenantId AND i.name IN (:names)")
    List<String> findAllResourceCrnsByNamesAndTenantId(@Param("names") Collection<String> names, @Param("tenantId") String tenantId);

    @Query("SELECT i.name FROM RecipeView i WHERE i.workspace.id = :workspaceId AND i.name IN (:names)")
    Set<String> findAllNamesByNamesAndWorkspaceId(@Param("names") Collection<String> names, @Param("workspaceId") Long workspaceId);

    @Query("SELECT i.resourceCrn FROM RecipeView i WHERE i.workspace.tenant.name = :tenantId AND i.name = :name")
    Optional<String> findResourceCrnByNameAndTenantId(@Param("name") String name, @Param("tenantId") String tenantId);

    @Query("SELECT i.name as name, i.resourceCrn as crn FROM RecipeView i WHERE i.workspace.tenant.name = :tenantId AND i.resourceCrn IN (:resourceCrns)")
    List<ResourceCrnAndNameView> findResourceNamesByCrnAndTenantId(@Param("resourceCrns") Collection<String> resourceCrns, @Param("tenantId") String tenantId);

    @Query("SELECT i.resourceCrn FROM RecipeView i WHERE i.workspace.tenant.name = :tenantId")
    List<String> findAllResourceCrnsByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(r.id, r.resourceCrn) FROM Recipe r WHERE r.workspace.id = :workspaceId " +
            "AND r.archived = false")
    List<ResourceWithId> findAsAuthorizationResourcesInWorkspace(@Param("workspaceId") Long workspaceId);

    @Query("SELECT r FROM RecipeView r "
            + "LEFT JOIN FETCH r.workspace w "
            + "LEFT JOIN FETCH w.tenant t "
            + "WHERE r.id IN :ids "
            + "AND r.archived = false")
    Set<RecipeView> findAllByIdNotArchived(@Param("ids") List<Long> ids);

}
