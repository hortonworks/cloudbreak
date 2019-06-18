package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.workspace.repository.check.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@DisableHasPermission
@EntityType(entityClass = Recipe.class)
@Transactional(TxType.REQUIRED)
@WorkspaceResourceType(resource = WorkspaceResource.RECIPE)
public interface RecipeRepository extends WorkspaceResourceRepository<Recipe, Long> {

    @CheckPermissionsByReturnValue
    Optional<Recipe> findByCrnAndWorkspaceId(String crn, Long workspaceId);

}
