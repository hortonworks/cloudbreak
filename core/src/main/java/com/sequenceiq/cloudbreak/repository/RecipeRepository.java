package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = Recipe.class)
@Transactional(TxType.REQUIRED)
public interface RecipeRepository extends WorkspaceResourceRepository<Recipe, Long> {

    Optional<Recipe> findByResourceCrnAndWorkspaceId(String crn, Long workspaceId);

}
