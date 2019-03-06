package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.READ;

import java.util.Collection;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = Recipe.class)
@Transactional(TxType.REQUIRED)
@WorkspaceResourceType(resource = WorkspaceResource.RECIPE)
public interface RecipeRepository extends WorkspaceResourceRepository<Recipe, Long> {

    @CheckPermissionsByWorkspaceId(action = READ, workspaceIdIndex = 1)
    @Query("SELECT r FROM Recipe r WHERE r. name in :names AND r.workspace.id = :workspaceId")
    Set<Recipe> findByNamesInWorkspace(@Param("names") Collection<String> names, @Param("workspaceId") Long workspaceId);

    @Override
    @CheckPermissionsByWorkspaceId(action = READ)
    @Query("SELECT r FROM Recipe r WHERE r.workspace.id = :workspaceId")
    Set<Recipe> findAllByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
